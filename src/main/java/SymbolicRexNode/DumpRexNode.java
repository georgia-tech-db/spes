package SymbolicRexNode;

import Z3Helper.z3Utility;
import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import org.apache.calcite.rex.RexNode;

import java.util.List;

public class DumpRexNode extends RexNodeBase {
    public DumpRexNode(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        Sort dumpSort = RexNodeUtility.convertRexNodeSort(z3Context,node);
        this.output = z3Utility.mkDumpSymbolicColumn();
    }
}
