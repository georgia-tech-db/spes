package SimpleQueryTests;

import AlgeNode.AlgeNode;
import AlgeNodeParser.AlgeNodeParserPair;
import AlgeRule.AlgeRule;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;

public class SimpleEmptyTable {
    public static void main(String[] args) throws  Exception{
        Context z3Context = new Context();
        simpleParser parser = new simpleParser();
        RelNode newNode = parser.getRelNode("SELECT * FROM EMP WHERE DEPTNO > 10 AND DEPTNO < 5");
        AlgeNode algeExpr = AlgeRule.normalize(AlgeNodeParserPair.constructAlgeNode(newNode,z3Context));
        System.out.println(algeExpr);
        simpleParser parser2 = new simpleParser();
        RelNode newNode2 = parser2.getRelNode("SELECT * FROM EMP WHERE DEPTNO > 7 AND DEPTNO < 4");
        AlgeNode algeExpr2 = AlgeRule.normalize(AlgeNodeParserPair.constructAlgeNode(newNode2,z3Context));
        System.out.println(algeExpr2);
        System.out.println(algeExpr.isEq(algeExpr2));
        //SimpleQueryTests.simpleParser parser2 = new SimpleQueryTests.simpleParser();
        //parser2.explain("SELECT * FROM EMP");
    }
}
