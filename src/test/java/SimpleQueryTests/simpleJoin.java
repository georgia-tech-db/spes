package SimpleQueryTests;

import AlgeNode.AlgeNode;
import AlgeNodeParser.AlgeNodeParserPair;
import com.microsoft.z3.Context;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;

public class simpleJoin {
    public static void main(String[] args) throws  Exception{
        Context z3Context = new Context();
        simpleParser parser = new simpleParser();
        RelNode newNode = parser.getRelNode("SELECT * FROM EMP INNER JOIN DEPT ON EMP.DEPTNO = DEPT.DEPTNO WHERE DEPT.DEPTNO > 10.");
        System.out.println(RelOptUtil.toString(newNode));
        AlgeNode algeExpr = AlgeNodeParserPair.constructAlgeNode(newNode,z3Context);
        System.out.println(algeExpr);
        simpleParser parser2 = new simpleParser();
        RelNode newNode2 = parser2.getRelNode("SELECT * FROM EMP INNER JOIN DEPT ON EMP.DEPTNO = DEPT.DEPTNO WHERE DEPT.DEPTNO > 10.");
        AlgeNode algeExpr2 = AlgeNodeParserPair.constructAlgeNode(newNode2,z3Context);
        System.out.println(algeExpr2);
        System.out.println(algeExpr.isEq(algeExpr2));
        //SimpleQueryTests.simpleParser parser2 = new SimpleQueryTests.simpleParser();
        //parser2.explain("SELECT * FROM EMP");
    }
}
