/*
 * Copyright (c) 2019 Walied K. Yassen, All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package me.waliedyassen.runescript.compiler.codegen;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.waliedyassen.runescript.compiler.ast.AstParameter;
import me.waliedyassen.runescript.compiler.ast.AstScript;
import me.waliedyassen.runescript.compiler.ast.expr.*;
import me.waliedyassen.runescript.compiler.ast.expr.literal.AstLiteralBool;
import me.waliedyassen.runescript.compiler.ast.expr.literal.AstLiteralInteger;
import me.waliedyassen.runescript.compiler.ast.expr.literal.AstLiteralLong;
import me.waliedyassen.runescript.compiler.ast.expr.literal.AstLiteralString;
import me.waliedyassen.runescript.compiler.ast.stmt.*;
import me.waliedyassen.runescript.compiler.ast.stmt.conditional.AstIfStatement;
import me.waliedyassen.runescript.compiler.ast.visitor.AstVisitor;
import me.waliedyassen.runescript.compiler.codegen.asm.*;
import me.waliedyassen.runescript.compiler.codegen.context.Context;
import me.waliedyassen.runescript.compiler.codegen.context.ContextType;
import me.waliedyassen.runescript.compiler.codegen.opcode.CoreOpcode;
import me.waliedyassen.runescript.compiler.codegen.opcode.Opcode;
import me.waliedyassen.runescript.compiler.stack.StackType;
import me.waliedyassen.runescript.compiler.symbol.SymbolTable;
import me.waliedyassen.runescript.compiler.symbol.impl.variable.VariableDomain;
import me.waliedyassen.runescript.compiler.type.Type;
import me.waliedyassen.runescript.compiler.type.tuple.TupleType;
import me.waliedyassen.runescript.compiler.util.trigger.TriggerType;

import java.util.Stack;

import static me.waliedyassen.runescript.compiler.codegen.opcode.CoreOpcode.BRANCH;

/**
 * Represents the compiler bytecode generator.
 *
 * @author Walied K. Yassen
 */
@RequiredArgsConstructor
public final class CodeGenerator implements AstVisitor {

    /**
     * The label generator used to generate any label for this code generator.
     */
    private final LabelGenerator labelGenerator = new LabelGenerator();

    /**
     * The blocks map of the current script.
     */
    @Getter
    private final BlockMap blockMap = new BlockMap();

    /**
     * The locals map of the current script.
     */
    private final LocalMap localMap = new LocalMap();

    /**
     * The symbol table which has all the information for the current generation.
     */
    private final SymbolTable symbolTable;

    /**
     * The instructions map which contains the primary instruction opcodes.
     */
    private final InstructionMap instructionMap;

    /**
     * The current block we are working on.
     */
    private final Stack<Context> contexts = new Stack<Context>();

    /**
     * Initialises the code generator and reset its state.
     */
    public void initialise() {
        labelGenerator.reset();
        blockMap.reset();
        localMap.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Script visit(AstScript script) {
        pushContext(ContextType.SCRIPT);
        var generated = new Script("[" + script.getTrigger().getText() + "," + script.getName().getText() + "]");
        for (var parameter : script.getParameters()) {
            parameter.accept(this);
        }
        bind(generateBlock("entry"));
        script.getCode().accept(this);
        popContext();
        return generated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Local visit(AstParameter parameter) {
        return localMap.registerParameter(parameter.getName().getText(), parameter.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstLiteralBool bool) {
        return instruction(CoreOpcode.PUSH_INT_CONSTANT, bool.getValue() ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstLiteralInteger integer) {
        return instruction(CoreOpcode.PUSH_INT_CONSTANT, integer.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstLiteralLong longInteger) {
        return instruction(CoreOpcode.PUSH_LONG_CONSTANT, longInteger.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstLiteralString string) {
        return instruction(CoreOpcode.PUSH_STRING_CONSTANT, string.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstConcatenation concatenation) {
        for (var expression : concatenation.getExpressions()) {
            expression.accept(this);
        }
        return instruction(CoreOpcode.JOIN_STRING, concatenation.getExpressions().length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstVariableExpression variableExpression) {
        var variable = variableExpression.getVariable();
        return instruction(getPushVariableOpcode(variable.getDomain(), variable.getType()), variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstGosub gosub) {
        for (var argument : gosub.getArguments()) {
            argument.accept(this);
        }
        var script = symbolTable.lookupScript(TriggerType.PROC, gosub.getName().getText());
        return instruction(CoreOpcode.GOSUB_WITH_PARAMS, script);
    }

    // TODO: AstDynamic code generation.

    /**
     * {@inheritDoc}
     */
    public Instruction visit(AstConstant constant) {
        var symbol = symbolTable.lookupConstant(constant.getName().getText());
        CoreOpcode opcode;
        switch (symbol.getType().getStackType()) {
            case INT:
                opcode = CoreOpcode.PUSH_INT_CONSTANT;
                break;
            case STRING:
                opcode = CoreOpcode.PUSH_STRING_CONSTANT;
                break;
            case LONG:
                opcode = CoreOpcode.PUSH_LONG_CONSTANT;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported constant base stack type: " + symbol.getType().getStackType());
        }
        return instruction(opcode, symbol.getValue());
    }

    /**
     * {@inheritDoc}
     */
    public Instruction visit(AstCommand command) {
        var symbol = symbolTable.lookupCommand(command.getName().getText());
        for (var argument : command.getArguments()) {
            argument.accept(this);
        }
        return instruction(symbol.getOpcode(), symbol.isAlternative() ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoreOpcode visit(AstBinaryOperation binaryOperation) {
        var operator = binaryOperation.getOperator();
        if (operator.isEquality() || operator.isRelational()) {
            CoreOpcode opcode;
            switch (operator) {
                case EQUAL:
                    opcode = CoreOpcode.BRANCH_EQUALS;
                    break;
                case LESS_THAN:
                    opcode = CoreOpcode.BRANCH_LESS_THAN;
                    break;
                case GREATER_THAN:
                    opcode = CoreOpcode.BRANCH_GREATER_THAN;
                    break;
                case LESS_THAN_OR_EQUALS:
                    opcode = CoreOpcode.BRANCH_LESS_THAN_OR_EQUALS;
                    break;
                case GREATER_THAN_OR_EQUALS:
                    opcode = CoreOpcode.BRANCH_GREATER_THAN_OR_EQUALS;
                    break;
                default:
                    throw new UnsupportedOperationException("Unexpected operator: " + operator);
            }
            binaryOperation.getLeft().accept(this);
            binaryOperation.getRight().accept(this);
            return opcode;
        } else {
            throw new UnsupportedOperationException("Unexpected operator: " + operator);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstVariableDeclaration variableDeclaration) {
        if (variableDeclaration.getExpression() != null) {
            variableDeclaration.getExpression().accept(this);
        } else {
            var opcode = getConstantOpcode(variableDeclaration.getType());
            instruction(opcode, variableDeclaration.getType().getDefaultValue());
        }
        var variable = variableDeclaration.getVariable();
        var local = localMap.registerVariable(variable.getName(), variable.getType());
        return instruction(getPopVariableOpcode(variable.getDomain(), variable.getType()), local);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstVariableInitializer variableInitializer) {
        variableInitializer.getExpression().accept(this);
        var variable = variableInitializer.getVariable();
        var local = variable.getDomain() == VariableDomain.LOCAL ? localMap.registerVariable(variable.getName(), variable.getType()) : variable;
        return instruction(getPopVariableOpcode(variable.getDomain(), variable.getType()), local);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object visit(AstIfStatement ifStatement) {
        // preserve the labels of this if statement for number order.
        var if_true_label = labelGenerator.generate("if_true");
        var if_else_label = labelGenerator.generate("if_else");
        var if_end_label = labelGenerator.generate("if_end");
        // store whether we have an else statement or not.
        var has_else = ifStatement.getFalseStatement() != null;
        // grab the parent block of the if statement.
        var source_block = context().getBlock();
        // generate the condition opcode of the if statement.
        var opcode = generateCondition(ifStatement.getCondition());
        // generate the branch instructions for the source block.
        instruction(source_block, opcode, if_true_label);
        instruction(source_block, BRANCH, has_else ? if_else_label : if_end_label);
        // generate the if-true block of the statement
        var true_block = bind(generateBlock(if_true_label));
        ifStatement.getTrueStatement().accept(this);
        // generate the branch instructions for the if-true block.
        instruction(true_block, BRANCH, if_end_label);
        // generate the if-else statement block and code.
        var else_block = has_else ? generateBlock(if_else_label) : null;
        if (has_else) {
            bind(else_block);
            ifStatement.getFalseStatement().accept(this);
            instruction(BRANCH, if_end_label);
        }
        // generate the if-end block and bind it.
        bind(generateBlock(if_end_label));
        return null;
    }

    /**
     * Performs code generation on the specified {@code condition} expression and returns it's associated {@link
     * CoreOpcode opcode}.
     *
     * @param condition
     *         the condition expression to perform the code generation on.
     *
     * @return the {@link CoreOpcode} of the generated condition code.
     */
    private CoreOpcode generateCondition(AstExpression condition) {
        if (condition instanceof AstBinaryOperation) {
            return visit((AstBinaryOperation) condition);
        } else {
            condition.accept(this);
            return CoreOpcode.BRANCH_IF_TRUE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstExpressionStatement expressionStatement) {
        var expression = expressionStatement.getExpression();
        expression.accept(this);
        var pushes = resolvePushCount(expression.getType());
        generateDiscard(pushes[0], pushes[1], pushes[2]);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instruction visit(AstReturnStatement returnStatement) {
        for (var expression : returnStatement.getExpressions()) {
            expression.accept(this);
        }
        return instruction(CoreOpcode.RETURN, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void visit(AstBlockStatement blockStatement) {
        for (var statement : blockStatement.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    /**
     * Generate a specific amount of discard instructions for each of the stack types.
     *
     * @param numInts
     *         the amount of integer discard instructions.
     * @param numStrings
     *         the amount of string discard instructions.
     * @param numLongs
     *         the amount of long discard instructions.
     */
    private void generateDiscard(int numInts, int numStrings, int numLongs) {
        if (numInts > 0) {
            for (int index = 0; index < numInts; index++) {
                instruction(CoreOpcode.POP_INT_DISCARD, 0);
            }
        }
        if (numStrings > 0) {
            for (int index = 0; index < numStrings; index++) {
                instruction(CoreOpcode.POP_STRING_DISCARD, 0);
            }
        }
        if (numLongs > 0) {
            for (int index = 0; index < numLongs; index++) {
                instruction(CoreOpcode.POP_LONG_DISCARD, 0);
            }
        }
    }

    /**
     * Resolves how many pushes of each stack type does the specified element {@link Type type} do.
     *
     * @param type
     *         the type to resolve for.
     *
     * @return the amount of pushes in one array in a specific order (int, string, long).
     */
    private int[] resolvePushCount(Type type) {
        var numInts = 0;
        var numStrings = 0;
        var numLongs = 0;
        if (type instanceof TupleType) {
            var flatten = ((TupleType) type).getFlattened();
            for (var elementType : flatten) {
                var stackType = elementType.getStackType();
                if (stackType == StackType.INT) {
                    numInts++;
                } else if (stackType == StackType.STRING) {
                    numStrings++;
                } else if (stackType == StackType.LONG) {
                    numLongs++;
                }
            }
        } else {
            var stackType = type.getStackType();
            if (stackType == StackType.INT) {
                numInts++;
            } else if (stackType == StackType.STRING) {
                numStrings++;
            } else if (stackType == StackType.LONG) {
                numLongs++;
            }
        }
        return new int[]{numInts, numStrings, numLongs};
    }

    /**
     * Creates a new {@link Instruction instruction} with the specified {@link CoreOpcode opcode} and the specified
     * {@code operand}. The {@link CoreOpcode opcode} will be remapped to a suitable regular {@link Opcode} instance
     * then passed to {@link #makeInstruction(Opcode, Object)} and then it gets added to the current active block in the
     * {@link #blockMap block map}.
     *
     * @param opcode
     *         the opcode of the instruction.
     * @param operand
     *         the operand of the instruction.
     *
     * @return the created {@link Instruction} object.
     */
    private Instruction instruction(CoreOpcode opcode, Object operand) {
        return instruction(context().getBlock(), opcode, operand);
    }

    /**
     * Creates a new {@link Instruction instruction} with the specified {@link CoreOpcode opcode} and the specified
     * {@code operand}. The {@link CoreOpcode opcode} will be remapped to a suitable regular {@link Opcode} instance
     * then passed to {@link #makeInstruction(Opcode, Object)} and then it gets added to the current active block in the
     * {@link #blockMap block map}.
     *
     * @param block
     *         the block to add the instruction to.
     * @param opcode
     *         the opcode of the instruction.
     * @param operand
     *         the operand of the instruction.
     *
     * @return the created {@link Instruction} object.
     */
    private Instruction instruction(Block block, CoreOpcode opcode, Object operand) {
        return instruction(block, instructionMap.lookup(opcode), operand);
    }

    /**
     * Creates a new {@link Instruction instruction} using {@link #makeInstruction(Opcode, Object)} and then adds it as
     * a child instruction to the current active block in the {@link #blockMap block map}.
     *
     * @param opcode
     *         the opcode of the instruction.
     * @param operand
     *         the operand of the instruction.
     *
     * @return the created {@link Instruction} object.
     */
    private Instruction instruction(Opcode opcode, Object operand) {
        return instruction(context().getBlock(), opcode, operand);
    }

    /**
     * Creates a new {@link Instruction instruction} using {@link #makeInstruction(Opcode, Object)} and then adds it as
     * a child instruction to the current active block in the {@link #blockMap block map}.
     *
     * @param block
     *         the block to add the instruction to.
     * @param opcode
     *         the opcode of the instruction.
     * @param operand
     *         the operand of the instruction.
     *
     * @return the created {@link Instruction} object.
     */
    private Instruction instruction(Block block, Opcode opcode, Object operand) {
        var instruction = makeInstruction(opcode, operand);
        block.add(instruction);
        return instruction;
    }

    /**
     * Creates a new {@link Instruction} object without linking it to any block.
     *
     * @param opcode
     *         the opcode of the instruction.
     * @param operand
     *         the operand of the instruction.
     *
     * @return the created {@link Instruction} object.
     */
    private Instruction makeInstruction(Opcode opcode, Object operand) {
        return new Instruction(opcode, operand);
    }

    /**
     * Binds the specified {@link Block block} as the current working block.
     *
     * @param block
     *         the block to bind as the working block.
     *
     * @return the block that was passed to the method.
     */
    private Block bind(Block block) {
        context().setBlock(block);
        return block;
    }

    /**
     * Generates a new {@link Block} object.
     *
     * @param name
     *         the name of the block label.
     *
     * @return the generated {@link Block} object.
     * @see BlockMap#generate(Label)
     */
    private Block generateBlock(String name) {
        return generateBlock(generateLabel(name));
    }

    /**
     * Generates a new {@link Block} object.
     *
     * @param label
     *         the label of the block.
     *
     * @return the generated {@link Block} object.
     * @see BlockMap#generate(Label)
     */
    private Block generateBlock(Label label) {
        return blockMap.generate(label);
    }

    /**
     * Generates a new unique {@link Label} object.
     *
     * @param name
     *         the name of the label.
     *
     * @return the generated {@link Label} object.
     * @see LabelGenerator#generate(String)
     */
    private Label generateLabel(String name) {
        return labelGenerator.generate(name);
    }

    /**
     * Gets the current active {@Link Context} object.
     *
     * @return the active {@link Context} object.
     */
    private Context context() {
        return contexts.lastElement();
    }

    /**
     * Creates a new {@Link Context} object and pushes it into the stack.
     *
     * @param type
     *         the type of the context.
     *
     * @return the created {@link Context} object.
     */
    private Context pushContext(ContextType type) {
        var context = new Context(type);
        contexts.push(context);
        return context;
    }

    /**
     * Pops the last context from the stack.
     *
     * @return the popped {@link Context} object.
     */
    private Context popContext() {
        return contexts.pop();
    }

    /**
     * Gets the push variable instruction {@link CoreOpcode opcode} of the specified {@link VariableDomain} and the
     * specified {@link Type}.
     *
     * @param domain
     *         the variable domain.
     * @param type
     *         the variable type.
     *
     * @return the instruction {@link CoreOpcode opcode} of that constant type.
     */
    private static CoreOpcode getPushVariableOpcode(VariableDomain domain, Type type) {
        switch (domain) {
            case LOCAL:
                switch (type.getStackType()) {
                    case INT:
                        return CoreOpcode.PUSH_INT_LOCAL;
                    case STRING:
                        return CoreOpcode.PUSH_STRING_LOCAL;
                    case LONG:
                        return CoreOpcode.PUSH_LONG_LOCAL;
                    default:
                        throw new UnsupportedOperationException("Unsupported local variable stack type: " + type.getStackType());

                }
            case PLAYER:
                return CoreOpcode.PUSH_VARP;
            case PLAYER_BIT:
                return CoreOpcode.PUSH_VARP_BIT;
            case CLIENT_INT:
                return CoreOpcode.PUSH_VARC_INT;
            case CLIENT_STRING:
                return CoreOpcode.PUSH_VARC_STRING;
            default:
                throw new UnsupportedOperationException("Unsupported variable domain: " + domain);
        }
    }

    /**
     * Gets the pop variable instruction {@link CoreOpcode opcode} of the specified {@link VariableDomain} and the
     * specified {@link Type}.
     *
     * @param domain
     *         the variable domain.
     * @param type
     *         the variable type.
     *
     * @return the instruction {@link CoreOpcode opcode} of that constant type.
     */
    private static CoreOpcode getPopVariableOpcode(VariableDomain domain, Type type) {
        switch (domain) {
            case LOCAL:
                switch (type.getStackType()) {
                    case INT:
                        return CoreOpcode.POP_INT_LOCAL;
                    case STRING:
                        return CoreOpcode.POP_STRING_LOCAL;
                    case LONG:
                        return CoreOpcode.POP_LONG_LOCAL;
                    default:
                        throw new UnsupportedOperationException("Unsupported local variable stack type: " + type.getStackType());
                }
            case PLAYER:
                return CoreOpcode.POP_VARP;
            case PLAYER_BIT:
                return CoreOpcode.POP_VARP_BIT;
            case CLIENT_INT:
                return CoreOpcode.POP_VARC_INT;
            case CLIENT_STRING:
                return CoreOpcode.POP_VARC_STRING;
            default:
                throw new UnsupportedOperationException("Unsupported variable domain: " + domain);
        }
    }

    /**
     * Gets the instruction {@link CoreOpcode} of the specified constant {@link Type}.
     *
     * @param type
     *         the type of the constant.
     *
     * @return the instruction {@link CoreOpcode opcode} of that constant type.
     */
    private static CoreOpcode getConstantOpcode(Type type) {
        switch (type.getStackType()) {
            case INT:
                return CoreOpcode.PUSH_INT_CONSTANT;
            case STRING:
                return CoreOpcode.PUSH_STRING_CONSTANT;
            case LONG:
                return CoreOpcode.PUSH_LONG_CONSTANT;
            default:
                throw new UnsupportedOperationException("Unsupported stack type: " + type.getStackType());
        }
    }
}
