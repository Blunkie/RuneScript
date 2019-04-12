/*
 * Copyright (c) 2018 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.compiler.ast.visitor;

import me.waliedyassen.runescript.compiler.ast.AstParameter;
import me.waliedyassen.runescript.compiler.ast.AstScript;
import me.waliedyassen.runescript.compiler.ast.expr.*;
import me.waliedyassen.runescript.compiler.ast.literal.*;
import me.waliedyassen.runescript.compiler.ast.stmt.*;
import me.waliedyassen.runescript.compiler.ast.stmt.conditional.AstIfStatement;
import me.waliedyassen.runescript.compiler.ast.stmt.conditional.AstWhileStatement;

/**
 * Represents a {@link AstVisitor} implementation that will visit every node in the AST tree while having access to when
 * each node has entered and when each node has left the visitor.
 */
public abstract class AstTreeVisitor implements AstVisitor<Void> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstScript script) {
        enter(script);
        for (var parameter : script.getParameters()) {
            parameter.accept(this);
        }
        script.getCode().accept(this);
        exit(script);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstScript} node.
     *
     * @param script
     *         the node we have just entered.
     */
    public void enter(AstScript script) {}


    /**
     * Gets called when we have just left an {@link AstScript} node.
     *
     * @param script
     *         the node we have just entered.
     */
    public void exit(AstScript script) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstParameter parameter) {
        enter(parameter);
        exit(parameter);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstParameter} node.
     *
     * @param parameter
     *         the node we have just entered.
     */
    public void enter(AstParameter parameter) {}


    /**
     * Gets called when we have just left an {@link AstParameter} node.
     *
     * @param parameter
     *         the node we have just entered.
     */
    public void exit(AstParameter parameter) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstBool bool) {
        enter(bool);
        exit(bool);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstBool} node.
     *
     * @param bool
     *         the node we have just entered.
     */
    public void enter(AstBool bool) {}

    /**
     * Gets called when we have just left an {@link AstBool} node.
     *
     * @param bool
     *         the node we have just entered.
     */
    public void exit(AstBool bool) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstInteger integer) {
        enter(integer);
        exit(integer);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstInteger} node.
     *
     * @param integer
     *         the node we have just entered.
     */
    public void enter(AstInteger integer) {}

    /**
     * Gets called when we have just left an {@link AstInteger} node.
     *
     * @param integer
     *         the node we have just entered.
     */
    public void exit(AstInteger integer) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstLong longInteger) {
        enter(longInteger);
        exit(longInteger);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstLong} node.
     *
     * @param longInteger
     *         the node we have just entered.
     */
    public void enter(AstLong longInteger) {}

    /**
     * Gets called when we have just left an {@link AstLong} node.
     *
     * @param longInteger
     *         the node we have just entered.
     */
    public void exit(AstLong longInteger) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstString string) {
        enter(string);
        exit(string);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstString} node.
     *
     * @param string
     *         the node we have just entered.
     */
    public void enter(AstString string) {}

    /**
     * Gets called when we have just left an {@link AstString} node.
     *
     * @param string
     *         the node we have just entered.
     */
    public void exit(AstString string) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstStringConcat concatenation) {
        enter(concatenation);
        for (var expression : concatenation.getExpressions()) {
            expression.accept(this);
        }
        exit(concatenation);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstStringConcat} node.
     *
     * @param concatenation
     *         the node we have just entered.
     */
    public void enter(AstStringConcat concatenation) {}

    /**
     * Gets called when we have just left an {@link AstStringConcat} node.
     *
     * @param concatenation
     *         the node we have just entered.
     */
    public void exit(AstStringConcat concatenation) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstVariableExpression variable) {
        enter(variable);
        exit(variable);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstVariableExpression} node.
     *
     * @param variable
     *         the node we have just entered.
     */
    public void enter(AstVariableExpression variable) {}

    /**
     * Gets called when we have just left an {@link AstVariableExpression} node.
     *
     * @param variable
     *         the node we have just entered.
     */
    public void exit(AstVariableExpression variable) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstGosub gosub) {
        enter(gosub);
        for (var expression : gosub.getArguments()) {
            expression.accept(this);
        }
        exit(gosub);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstGosub} node.
     *
     * @param gosub
     *         the node we have just entered.
     */
    public void enter(AstGosub gosub) {}

    /**
     * Gets called when we have just left an {@link AstGosub} node.
     *
     * @param gosub
     *         the node we have just entered.
     */
    public void exit(AstGosub gosub) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstDynamic dynamic) {
        enter(dynamic);
        exit(dynamic);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstDynamic} node.
     *
     * @param dynamic
     *         the node we have just entered.
     */
    public void enter(AstDynamic dynamic) {}

    /**
     * Gets called when we have just left an {@link AstDynamic} node.
     *
     * @param dynamic
     *         the node we have just entered.
     */
    public void exit(AstDynamic dynamic) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstConstant constant) {
        enter(constant);
        exit(constant);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstConstant} node.
     *
     * @param constant
     *         the node we have just entered.
     */
    public void enter(AstConstant constant) {}

    /**
     * Gets called when we have just left an {@link AstConstant} node.
     *
     * @param constant
     *         the node we have just entered.
     */
    public void exit(AstConstant constant) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstCommand command) {
        enter(command);
        for (var expression : command.getArguments()) {
            expression.accept(this);
        }
        exit(command);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstCommand} node.
     *
     * @param command
     *         the node we have just entered.
     */
    public void enter(AstCommand command) {}

    /**
     * Gets called when we have just left an {@link AstCommand} node.
     *
     * @param command
     *         the node we have just entered.
     */
    public void exit(AstCommand command) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstBinaryOperation binaryOperation) {
        enter(binaryOperation);
        binaryOperation.getLeft().accept(this);
        binaryOperation.getRight().accept(this);
        exit(binaryOperation);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstBinaryOperation} node.
     *
     * @param binaryOperation
     *         the node we have just entered.
     */
    public void enter(AstBinaryOperation binaryOperation) {}

    /**
     * Gets called when we have just left an {@link AstBinaryOperation} node.
     *
     * @param binaryOperation
     *         the node we have just entered.
     */
    public void exit(AstBinaryOperation binaryOperation) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstVariableDeclaration variableDeclaration) {
        enter(variableDeclaration);
        variableDeclaration.getExpression().accept(this);
        exit(variableDeclaration);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstVariableDeclaration} node.
     *
     * @param variableDeclaration
     *         the node we have just entered.
     */
    public void enter(AstVariableDeclaration variableDeclaration) {}

    /**
     * Gets called when we have just left an {@link AstVariableDeclaration} node.
     *
     * @param variableDeclaration
     *         the node we have just entered.
     */
    public void exit(AstVariableDeclaration variableDeclaration) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstVariableInitializer variableInitializer) {
        enter(variableInitializer);
        variableInitializer.getExpression().accept(this);
        exit(variableInitializer);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstVariableInitializer} node.
     *
     * @param variableInitializer
     *         the node we have just entered.
     */
    public void enter(AstVariableInitializer variableInitializer) {}

    /**
     * Gets called when we have just left an {@link AstVariableInitializer} node.
     *
     * @param variableInitializer
     *         the node we have just entered.
     */
    public void exit(AstVariableInitializer variableInitializer) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstSwitchStatement switchStatement) {
        enter(switchStatement);
        switchStatement.getCondition().accept(this);
        for (var switchCase : switchStatement.getCases()) {
            switchCase.accept(this);
        }
        if (switchStatement.getDefaultCase() != null) {
            switchStatement.accept(this);
        }
        exit(switchStatement);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstSwitchStatement} node.
     *
     * @param switchStatement
     *         the node we have just entered.
     */
    public void enter(AstSwitchStatement switchStatement) {}

    /**
     * Gets called when we have just left an {@link AstSwitchStatement} node.
     *
     * @param switchStatement
     *         the node we have just entered.
     */
    public void exit(AstSwitchStatement switchStatement) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstSwitchCase switchCase) {
        enter(switchCase);
        for (var expression : switchCase.getKeys()) {
            expression.accept(this);
        }
        switchCase.getCode().accept(this);
        exit(switchCase);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstSwitchCase} node.
     *
     * @param switchCase
     *         the node we have just entered.
     */
    public void enter(AstSwitchCase switchCase) {}

    /**
     * Gets called when we have just left an {@link AstSwitchCase} node.
     *
     * @param switchCase
     *         the node we have just entered.
     */
    public void exit(AstSwitchCase switchCase) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstIfStatement ifStatement) {
        enter(ifStatement);
        ifStatement.getCondition().accept(this);
        ifStatement.getTrueStatement().accept(this);
        if (ifStatement.getFalseStatement() != null) {
            ifStatement.getFalseStatement().accept(this);
        }
        exit(ifStatement);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstIfStatement} node.
     *
     * @param ifStatement
     *         the node we have just entered.
     */
    public void enter(AstIfStatement ifStatement) {}

    /**
     * Gets called when we have just left an {@link AstIfStatement} node.
     *
     * @param ifStatement
     *         the node we have just entered.
     */
    public void exit(AstIfStatement ifStatement) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstWhileStatement whileStatement) {
        enter(whileStatement);
        whileStatement.getCondition().accept(this);
        whileStatement.getCode().accept(this);
        exit(whileStatement);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstWhileStatement} node.
     *
     * @param whileStatement
     *         the node we have just entered.
     */
    public void enter(AstWhileStatement whileStatement) {}

    /**
     * Gets called when we have just left an {@link AstWhileStatement} node.
     *
     * @param whileStatement
     *         the node we have just entered.
     */
    public void exit(AstWhileStatement whileStatement) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstExpressionStatement expressionStatement) {
        enter(expressionStatement);
        expressionStatement.getExpression().accept(this);
        exit(expressionStatement);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstExpressionStatement} node.
     *
     * @param expressionStatement
     *         the node we have just entered.
     */
    public void enter(AstExpressionStatement expressionStatement) {}

    /**
     * Gets called when we have just left an {@link AstExpressionStatement} node.
     *
     * @param expressionStatement
     *         the node we have just entered.
     */
    public void exit(AstExpressionStatement expressionStatement) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstReturnStatement returnStatement) {
        enter(returnStatement);
        for (var expression : returnStatement.getExpressions()) {
            expression.accept(this);
        }
        exit(returnStatement);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstReturnStatement} node.
     *
     * @param returnStatement
     *         the node we have just entered.
     */
    public void enter(AstReturnStatement returnStatement) {}

    /**
     * Gets called when we have just left an {@link AstReturnStatement} node.
     *
     * @param returnStatement
     *         the node we have just entered.
     */
    public void exit(AstReturnStatement returnStatement) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstBlockStatement blockStatement) {
        enter(blockStatement);
        for (var statement : blockStatement.getStatements()) {
            statement.accept(this);
        }
        exit(blockStatement);
        return null;
    }

    /**
     * Gets called when we have just entered an {@link AstBlockStatement} node.
     *
     * @param blockStatement
     *         the node we have just entered.
     */
    public void enter(AstBlockStatement blockStatement) {}

    /**
     * Gets called when we have just left an {@link AstBlockStatement} node.
     *
     * @param blockStatement
     *         the node we have just entered.
     */
    public void exit(AstBlockStatement blockStatement) {}
}