package AlgeNodeParser;

import AlgeNode.AlgeNode;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;

public abstract class AlgeNodeParser {
    abstract public AlgeNode constructRelNode(RelNode input, Context z3Context);
}
