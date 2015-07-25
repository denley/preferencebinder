package me.denley.preferencebinder.internal;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

/**
 * A code analyzer used to check for missing calls to PreferenceBinder
 * setup and teardown method calls.
 */
public class BindingCallCodeAnalyzer extends TreePathScanner<Object, Trees> {

    public static final String STATEMENT_BIND = "^PreferenceBinder\\.bind\\(.*\\);$";
    public static final String STATEMENT_UNBIND = "^PreferenceBinder\\.unbind\\(.*\\);$";


    private boolean foundCall = false;
    private final String statementRegex;

    BindingCallCodeAnalyzer(final String statementRegex) {
        this.statementRegex = statementRegex;
    }

    boolean didFindCall() {
        return foundCall;
    }

    @Override public Object visitMethod(final MethodTree node, final Trees trees) {
        for(StatementTree statement : node.getBody().getStatements()) {
            if(statement.getKind() == Tree.Kind.EXPRESSION_STATEMENT && statement.toString().matches(statementRegex)) {
                foundCall = true;
                break;
            }
        }
        return super.visitMethod(node, trees);
    }

}
