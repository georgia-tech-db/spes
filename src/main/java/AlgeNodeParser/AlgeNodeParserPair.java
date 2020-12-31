package AlgeNodeParser;

import AlgeNode.AlgeNode;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.logical.*;

public enum AlgeNodeParserPair {
    Projection(LogicalProject.class,ProjectParser.class),
    Table(TableScan.class,TableParser.class),
    Filter(LogicalFilter.class,FilterParser.class),
    Join(LogicalJoin.class, JoinParser.class),
    Aggregate(LogicalAggregate.class,AggParser.class),
    Union(LogicalUnion.class,UnionParser.class)
    ;
    private final Class relNode;
    private final Class parserClass;

    AlgeNodeParserPair(Class relNode, Class converterClass) {
        this.relNode = relNode;
        this.parserClass = converterClass;
    }

    public Class getRelNode() {
        return this.relNode;
    }

    public Class getParserClass() {
        return this.parserClass;
    }

    public static AlgeNode constructAlgeNode(RelNode input,Context z3Context) {
        for (AlgeNodeParserPair parserPair : AlgeNodeParserPair.values()) {
            if (parserPair.getRelNode().isInstance(input)) {
                AlgeNodeParser parser = null;
                try {
                    parser = (AlgeNodeParser) parserPair.getParserClass().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    System.out.println("here is an exception");
                    System.out.println(e);
                }
                if (parser != null) {
                    return parser.constructRelNode(input,z3Context);
                }
            }
        }
        System.out.println("this class: "+input.getClass()+" has not been handled in construct algeNode");
        return null;
    }
}
