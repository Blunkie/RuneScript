/*
 * Copyright (c) 2020 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.editor.project.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.waliedyassen.runescript.compiler.CompileInput;
import me.waliedyassen.runescript.compiler.CompileResult;
import me.waliedyassen.runescript.compiler.ast.AstScript;
import me.waliedyassen.runescript.compiler.ast.visitor.AstTreeVisitor;
import me.waliedyassen.runescript.compiler.symbol.impl.script.ScriptInfo;
import me.waliedyassen.runescript.editor.job.WorkExecutor;
import me.waliedyassen.runescript.editor.project.Project;
import me.waliedyassen.runescript.editor.project.dependency.DependencyTree;
import me.waliedyassen.runescript.editor.util.ChecksumUtil;
import me.waliedyassen.runescript.editor.util.ex.PathEx;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the cache of a specific project.
 *
 * @author Walied K. Yassen
 */
@Slf4j
public final class Cache {

    /**
     * A map of all the cached files in the project.
     */
    @Getter
    private final Map<String, CachedFile> filesByPath = new HashMap<>();

    /**
     * A map of all the declarations names associated by their container file.
     */
    @Getter
    private final Map<String, CachedFile> filesByDeclaration = new HashMap<>();

    /**
     * The cached dependency tree of the project.
     */
    private final DependencyTree<String> dependencyTree = new DependencyTree<>();

    /**
     * The project which this cache is for.
     */
    private final Project project;

    /**
     * The save task future of the cache.
     */
    @SuppressWarnings("unused")
    private final ScheduledFuture<?> saveTaskFuture;

    /**
     * Whether or not the cache is currently dirty and needs saving.
     */
    private volatile boolean dirty;

    // TODO: Verify the commands, instructions, and triggers are the same.
    // TODO: Build a dependency tree for all of the scripts.

    /**
     * Constructs a new {@link Cache} type object instance.
     *
     * @param project the project which this cache is for.
     */
    public Cache(Project project) {
        this.project = project;
        saveTaskFuture = WorkExecutor.getSingleThreadScheduler().scheduleWithFixedDelay(this::performSaving, 5000L, 5000L, TimeUnit.MILLISECONDS);
    }

    /**
     * Performs the saving task of the cache.
     */
    public void performSaving() {
        if (!dirty) {
            return;
        }
        project.saveCache();
        dirty = false;
    }

    /**
     * Deserialises the cache content form the specified {@link DataInputStream}.
     *
     * @param stream the stream to deserialise the cache content from.
     * @throws IOException if anything occurs while reading data from the specified stream.
     */
    public void read(DataInputStream stream) throws IOException {
        var filesCount = stream.readInt();
        for (var index = 0; index < filesCount; index++) {
            var cachedFile = new CachedFile();
            cachedFile.read(project.getCompiler().getEnvironment(), stream);
            addCachedFile(cachedFile);
        }
        var dependencyParentCount = stream.readInt();
        for (var parentIndex = 0; parentIndex < dependencyParentCount; parentIndex++) {
            var node = dependencyTree.findOrCreate(stream.readUTF());
            var dependencyChildCount = stream.readUnsignedShort();
            for (var childIndex = 0; childIndex < dependencyChildCount; childIndex++) {
                node.addDependency(stream.readUTF());
            }
        }
    }

    /**
     * Serialises the cache content form the specified {@link DataOutputStream}.
     *
     * @param stream the stream to serialise the cache content into.
     * @throws IOException if anything occurs while writing data to the specified stream.
     */
    public void write(DataOutputStream stream) throws IOException {
        stream.writeInt(filesByPath.size());
        for (var file : filesByPath.values()) {
            file.write(stream);
        }
        stream.writeInt(dependencyTree.size());
        for (var node : dependencyTree.valueSet()) {
            stream.writeUTF(node.getKey());
            stream.writeShort(node.getDependsOn().size());
            for (var dependency : node.getDependsOn().values()) {
                stream.writeUTF(dependency.getKey());
            }
        }
    }

    /**
     * Compares this cache against the content of the specified source directory.
     *
     * @param sourceDirectory the source directory to resolve the files from.
     * @throws IOException if anything occurs walking through the path tree or while writing the changes to the disk.
     */
    public void diff(Path sourceDirectory) throws IOException {
        var paths = Files.walk(sourceDirectory).filter(Files::isRegularFile).collect(Collectors.toList());
        var visited = new HashSet<String>();
        var input = new CompileInput();
        var modified = false;
        for (var path : paths) {
            var key = PathEx.normaliseToString(sourceDirectory, path);
            visited.add(key);
            var cachedFile = filesByPath.get(key);
            if (cachedFile == null) {
                cachedFile = new CachedFile();
                cachedFile.setPath(PathEx.normaliseToString(sourceDirectory, path.getParent()));
                cachedFile.setName(path.getFileName().toString());
                addCachedFile(cachedFile);
            }
            var diskData = Files.readAllBytes(path);
            var diskCrc = ChecksumUtil.calculateCrc32(diskData);
            if (cachedFile.getCrc() == diskCrc) {
                continue;
            }
            clearCachedFile(cachedFile);
            undeclareSymbols(cachedFile);
            input.addSourceCode(cachedFile, diskData);
            cachedFile.setCrc(diskCrc);
            modified = true;
        }
        input.addVisitor(new DependencyTreeBuilder(dependencyTree));
        var result = project.getCompiler().compile(input);
        for (var pair : result.getErrors()) {
            var cachedFile = (CachedFile) pair.getKey();
            cachedFile.getErrors().add(new CachedError(pair.getValue().getRange(), pair.getValue().getMessage()));
        }
        for (var pair : result.getScripts()) {
            var cachedFile = (CachedFile) pair.getKey();
            addScript(cachedFile, pair.getValue().getInfo());
        }
        var deletedFiles = filesByPath.keySet().stream().filter(file -> !visited.contains(file)).collect(Collectors.toSet());
        modified |= deletedFiles.size() > 0;
        for (var deletedFile : deletedFiles) {
            var cachedFile = filesByPath.get(deletedFile);
            removeCachedFile(cachedFile);
            undeclareSymbols(cachedFile);
        }
        filesByPath.values().forEach(this::declareSymbols);
        if (modified) {
            project.saveCache();
        }
    }

    /**
     * Attempts to recompile the file at the specified {@link Path} and has the specified {@code data}.
     *
     * @param path the path which leads to the file that we want to recompile.
     * @param data the source code data of the file that we want to recompile.
     * @return the result of the re-compilation process.
     */
    public CompileResult recompile(Path path, byte[] data) {
        var key = PathEx.normaliseToString(project.getBuildPath().getSourceDirectory(), path);
        var cachedFile = filesByPath.get(key);
        if (cachedFile == null) {
            cachedFile = new CachedFile();
            cachedFile.setPath(PathEx.normaliseToString(project.getBuildPath().getSourceDirectory(), path.getParent()));
            cachedFile.setName(path.getFileName().toString());
            filesByPath.put(key, cachedFile);
        }
        undeclareSymbols(cachedFile);
        var changedDecls = cachedFile.getScripts().stream().collect(Collectors.toMap(ScriptInfo::getFullName, Function.identity()));
        clearCachedFile(cachedFile);
        var input = CompileInput.of(null, data);
        input.addVisitor(new DependencyTreeBuilder(dependencyTree));
        CompileResult result;
        try {
            result = project.getCompiler().compile(input);
        } catch (IOException e) {
            log.error("An I/O error occurred while updating a script", e);
            return null;
        }
        for (var pair : result.getScripts()) {
            var script = pair.getValue();
            cachedFile.getScripts().add(script.getInfo());
            var decl = changedDecls.get(script.getName());
            if (decl != null && decl.equalSignature(script.getInfo())) {
                changedDecls.remove(script.getName());
            }
            filesByDeclaration.put(script.getName(), cachedFile);
        }
        if (!changedDecls.isEmpty()) {
            var affectedScripts = new HashSet<String>();
            for (var changedDecl : changedDecls.keySet()) {
                var node = dependencyTree.find(changedDecl);
                if (node == null) {
                    continue;
                }
                affectedScripts.addAll(node.getUsedBy().keySet());
            }
            var affectedCachedFiles = affectedScripts
                    .stream()
                    .map(filesByDeclaration::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            for (var affectedCachedFile : affectedCachedFiles) {
                var affectedFilePath = project.getBuildPath().getSourceDirectory().resolve(affectedCachedFile.getFullPath());
                if (Files.exists(affectedFilePath)) {
                    try {
                        recompile(affectedFilePath, Files.readAllBytes(affectedFilePath));
                    } catch (IOException e) {
                        log.error("Failed to read the file content from the disk", e);
                    }
                }
            }
        }
        for (var error : result.getErrors()) {
            cachedFile.getErrors().add(new CachedError(error.getValue().getRange(), error.getValue().getMessage()));
        }

        declareSymbols(cachedFile);
        cachedFile.setCrc(ChecksumUtil.calculateCrc32(data));
        project.updateErrors(path);
        dirty = true;
        return result;
    }

    /**
     * Attempts to recompile the file at the specified {@link Path} and has the specified {@code data}. This is same
     * as {@link #recompile(Path, byte[])} except that this does not save or alter the state of the cache.
     *
     * @param path the path which leads to the file that we want to recompile.
     * @param data the source code data of the file that we want to recompile.
     * @return the result of the re-compilation process.
     */
    public CompileResult recompileNonPersistent(Path path, byte[] data) {
        var key = PathEx.normaliseToString(project.getBuildPath().getSourceDirectory(), path);
        var cachedFile = filesByPath.get(key);
        if (cachedFile != null) {
            for (var script : cachedFile.getScripts()) {
                project.getCompiler().getSymbolTable().undefineScript(script.getTrigger(), script.getName());
            }
        }
        try {
            return project.getCompiler().compile(CompileInput.of(null, data));
        } catch (IOException e) {
            log.error("An I/O error occurred while compiling a script non persistently", e);
            return null;
        } finally {
            if (cachedFile != null) {
                for (var script : cachedFile.getScripts()) {
                    project.getCompiler().getSymbolTable().defineScript(script);
                }
            }
        }
    }

    /**
     * Declares all of the symbols that are in the cached file in the symbol table.
     *
     * @param cachedFile the cached file to declare all of the symbols that are in.
     */
    public void declareSymbols(CachedFile cachedFile) {
        for (var script : cachedFile.getScripts()) {
            project.getCompiler().getSymbolTable().defineScript(script);
        }
    }

    /**
     * Undeclares all of the symbols that are in the cached file from the symbol table.
     *
     * @param cachedFile the cached file to undeclare all of the symbols that are in.
     */
    public void undeclareSymbols(CachedFile cachedFile) {
        for (var script : cachedFile.getScripts()) {
            project.getCompiler().getSymbolTable().undefineScript(script.getTrigger(), script.getName());
            dependencyTree.remove(script.getFullName());
        }
    }

    private void addCachedFile(CachedFile cachedFile) {
        filesByPath.put(cachedFile.getFullPath(), cachedFile);
        for (var script : cachedFile.getScripts()) {
            filesByDeclaration.put(script.getFullName(), cachedFile);
        }
    }

    private void removeCachedFile(CachedFile cachedFile) {
        filesByPath.remove(cachedFile.getFullPath());
        for (var script : cachedFile.getScripts()) {
            filesByDeclaration.remove(script.getFullName());
        }
    }

    private void clearCachedFile(CachedFile cachedFile) {
        for (var script : cachedFile.getScripts()) {
            filesByDeclaration.remove(script.getFullName());
        }
        cachedFile.getScripts().clear();
        cachedFile.getErrors().clear();
    }

    private void addScript(CachedFile cachedFile, ScriptInfo info) {
        cachedFile.getScripts().add(info);
        filesByDeclaration.put(info.getFullName(), cachedFile);
    }

    private static final class CompilationUnitCollector extends AstTreeVisitor {

        @Override
        public Void visit(AstScript script) {
            var name = script.getFullName();
            return super.visit(script);
        }
    }
}
