package Z3Helper;
import SymbolicRexNode.RexNodeUtility;
import SymbolicRexNode.SymbolicColumn;
import com.microsoft.z3.*;
import org.apache.calcite.rel.type.RelDataType;

import java.util.*;
import java.util.stream.Collectors;


public class z3Utility {
    static private int count = 0;

    static public void reset(){
        count = 0;
    }

    static public boolean isConditionEq(List<BoolExpr> assignConstraints, BoolExpr condition1, BoolExpr condition2, Context z3Context) {
        BoolExpr[] equation = new BoolExpr[assignConstraints.size()+1];
        assignConstraints.toArray(equation);
        equation[assignConstraints.size()] = z3Context.mkNot(z3Context.mkEq(condition1,condition2));
        Solver s = z3Context.mkSolver();
        s.add(equation);
        return s.check() == Status.UNSATISFIABLE;
    }

    static public BoolExpr makeColumnsEq(List<SymbolicColumn> columns1,List<SymbolicColumn> columns2,Context z3Context){
        if (columns1.size() == columns2.size()) {
            BoolExpr[] columnsEq = new BoolExpr[columns1.size()];
            for (int i=0; i<columns1.size();i++){
                columnsEq[i] = simpleColumnEq(columns1.get(i),columns2.get(i),z3Context);
            }
            return (BoolExpr) z3Context.mkAnd(columnsEq).simplify();
        }
        return null;

    }
    static public BoolExpr simpleColumnEq(SymbolicColumn column1, SymbolicColumn column2, Context z3Context){
        BoolExpr valueEqual = (BoolExpr) z3Context.mkEq(column1.getSymbolicValue(),column2.getSymbolicValue()).simplify();
        BoolExpr isNullEqual = (BoolExpr) z3Context.mkEq(column1.getSymbolicNull(),column2.getSymbolicNull()).simplify();
        return (BoolExpr) z3Context.mkAnd(valueEqual,isNullEqual).simplify();
    }

    static public boolean symbolicOutputEqual(BoolExpr conditions, List<SymbolicColumn> list1, List<SymbolicColumn> list2, Context z3Context){
        if(list1.size() == list2.size()) {
            List<BoolExpr> columnEqs = new ArrayList<>();
            for(int i=0; i<list1.size(); i++){
                BoolExpr columnEq = symbolicColumnEq(list1.get(i),list2.get(i),z3Context,true);
                if (columnEq != null){
                    columnEqs.add(columnEq);
                }
            }
            if (columnEqs.isEmpty()){
                return true;
            }
            BoolExpr notEq = (BoolExpr) z3Context.mkNot(z3Utility.mkAnd(columnEqs,z3Context)).simplify();
            Solver s = z3Context.mkSolver();
            s.add((BoolExpr) z3Context.mkAnd(conditions, notEq).simplify());
            return s.check() == Status.UNSATISFIABLE ;
        }
        return false;
    }

    static public Expr getDumpVariable(Sort sort,Context z3Context){
        count++;
        return z3Context.mkConst("dump"+count,sort);
    }

    static public Expr mkDumpValue(RelDataType type,Context z3Context){
        count++;
        return z3Context.mkConst("dump"+count,RexNodeUtility.getSortBasedOnSqlType(z3Context,type.getSqlTypeName()));
    }

    static public BoolExpr mkBoolPredicate(Context z3Context){
        count++;
        String name = "B"+count;
        BoolExpr newVariable = z3Context.mkBoolConst(name);
        return newVariable;
    }

    public static BoolExpr mkAnd(List<BoolExpr> constraints,Context z3Context){
        BoolExpr[] andC = new BoolExpr[constraints.size()];
        constraints.toArray(andC);
        return (BoolExpr) z3Context.mkAnd(andC).simplify();
    }

    public static BoolExpr mkOr(List<BoolExpr> constraints,Context z3Context){
        BoolExpr[] orC = new BoolExpr[constraints.size()];
        constraints.toArray(orC);
        return (BoolExpr) z3Context.mkOr(orC).simplify();
    }

    public static boolean isUnsat(BoolExpr expr, Context z3Context){
        Solver s = z3Context.mkSolver();
        s.add(expr);
        return s.check() == Status.UNSATISFIABLE;
    }

    static public BoolExpr symbolicColumnEq(SymbolicColumn column1,SymbolicColumn column2,Context z3Context,boolean checkTrivialEq){
        if (checkTrivialEq&&trivialEqual(column1,column2)){
            return null;
        }
        BoolExpr bothNull = z3Context.mkAnd(column1.getSymbolicNull(),column2.getSymbolicNull());
        BoolExpr valueEq = z3Context.mkAnd(z3Context.mkEq(column1.getSymbolicNull(),column2.getSymbolicNull()),z3Context.mkEq(column1.getSymbolicValue(),column2.getSymbolicValue()));
        return (BoolExpr) z3Context.mkOr(bothNull,valueEq).simplify();
    }

    static private boolean trivialEqual (SymbolicColumn column1, SymbolicColumn column2){
        if (trivialEqual(column1.getSymbolicValue(),column2.getSymbolicValue())){
            if (trivialEqual(column1.getSymbolicNull(),column2.getSymbolicNull())){
                return true;
            }
        }
        return false;

    }
    //TODO
    public static SymbolicColumn mkDumpSymbolicColumn(){
        return null;
    }

    public static List<Expr> collectAllConstantVariable(Expr e){
        if (isVariable(e)) {
            return Collections.singletonList(e);
        } else {
            List<Expr> result = new ArrayList<>();
            for (Expr arg : e.getArgs()) {
                result.addAll(collectAllConstantVariable(arg));
            }
            return result;
        }
    }

    public static Expr constructFreshExpr(Expr e,Context z3Context){
        List<Expr> variables = collectAllConstantVariable(e);
        Expr[] oldVariables = new Expr[variables.size()];
        Expr[] newVariables = new Expr[variables.size()];
        for(int i=0; i < variables.size(); i++){
            Expr variable = variables.get(i);
            String name = variable.getSExpr();
            Expr freshVariable = z3Context.mkConst(name+"Fresh",variable.getSort());
            oldVariables[i] = variable;
            newVariables[i] = freshVariable;
        }
        return e.substitute(oldVariables,newVariables);
    }

    public static boolean isVariable(Expr e){
        return e.isConst() && (!isConstant(e));
    }

    public static boolean isConstant(Expr e){
        return e.isTrue() || e.isFalse() || e.isIntNum() || e.isRatNum();
    }

    static private boolean trivialEqual (Expr e1, Expr e2) {
        if (e1.isTrue() && e2.isTrue()){
            return true;
        }else if (e1.isFalse() && e2.isFalse()){
            return true;
        } else  if (e1.isRatNum() && e2.isRatNum()) {
            RatNum e1V = (RatNum) e1;
            RatNum e2V = (RatNum) e2;
            return e1V.getBigIntNumerator().equals(e2V.getBigIntDenominator());
        } else if (e1.isIntNum() && e2.isIntNum()){
            IntNum e1V = (IntNum) e1;
            IntNum e2V = (IntNum) e2;
            return e1V.getBigInteger().equals(e2V.getBigInteger());
        } else if (e1.isConst() && e2.isConst()){
            return e1.getSExpr().equals(e2.getSExpr());
        } else {
            return false;
        }
    }
}
