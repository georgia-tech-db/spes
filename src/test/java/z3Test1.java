import Z3Helper.z3Utility;
import com.microsoft.z3.*;

public class z3Test1 {
    public static void main(String[] args) {
        Context z3 = new Context();
        IntExpr zero = z3.mkInt(0);
        IntExpr one = z3.mkInt(1);
        IntExpr variable = z3.mkIntConst("a");
        IntExpr variable2 = z3.mkIntConst("c");
        IntExpr variable3 = z3.mkIntConst("b");
        System.out.println(variable.toString());
        BoolExpr greater = z3.mkGe(variable,zero);
        BoolExpr lessthan = z3.mkLt(variable2,one);
        BoolExpr formula = z3.mkAnd(greater,lessthan);
        System.out.println(greater);
        Expr[] copy =new Expr[2];
        copy[0] = variable;
        copy[1] = variable2;
        Expr[] beCopied =new Expr[2];
        beCopied[0] = variable3;
        beCopied[1] = variable3;
        System.out.println(formula);
        System.out.println(z3Utility.constructFreshExpr(formula,z3));
        Solver s = z3.mkSolver();
        s.add(greater);
        Status result = s.check();
        System.out.println(result);

    }
}
