package SymbolicRexNode;

import Z3Helper.z3Utility;
import com.microsoft.z3.*;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDeFun extends RexNodeBase {
    public class SymbolicUserDefun {
        private String funName;
        private List<SymbolicColumn> args;
        private SymbolicColumn returnValue;
        public SymbolicUserDefun (String funName,List<SymbolicColumn> args,SymbolicColumn returnValue){
            this.funName = funName;
            this.args = args;
            this.returnValue = returnValue;
        }

        public SymbolicColumn getReturnValue () {
            return this.returnValue;
        }

        public boolean isEq (String funName, List<SymbolicColumn> args){
            if (this.funName.equals(funName)){
                boolean result = z3Utility.symbolicOutputEqual(z3Context.mkTrue(),args,this.args,z3Context);
                return result;
            }
            return false;
        }
    }

    static private Map<String,List<SymbolicUserDefun>> registerFunction = new HashMap<>();

    static public void reset1 (){
        registerFunction = new HashMap<>();
    }

    public UserDeFun(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);
    }

    private void buildExpr(RexNode node){
        RexCall functionNode = (RexCall) node;
        String funName = functionNode.getOperator().getName();
        List<RexNode> parameters = functionNode.getOperands();
        List<SymbolicColumn> args = new ArrayList<>();
        for(int i=0;i<parameters.size();i++){
            RexNode parameter = parameters.get(i);
            RexNodeBase converter = RexNodeConverter.rexConstraints(inputs,parameter,z3Context);
            args.add(converter.getOutput());
        }
        if (registerFunction.containsKey(funName)){
            List<SymbolicUserDefun> oldFuns = registerFunction.remove(funName);
            boolean findMatch = false;
            for (SymbolicUserDefun oldFun : oldFuns ){
                if (oldFun.isEq(funName,args)){
                    this.output = oldFun.getReturnValue();
                    findMatch = true;
                    break;
                }
            }
            if (!findMatch) {
                this.output = SymbolicColumn.mkNewSymbolicColumn(z3Context,node.getType());
                SymbolicUserDefun symbolicUserDefun = new SymbolicUserDefun(funName,args,this.output);
                oldFuns.add(symbolicUserDefun);
            }
            registerFunction.put(funName,oldFuns);

        }else {
            List<SymbolicUserDefun> oldFuns = new ArrayList<>();
            this.output = SymbolicColumn.mkNewSymbolicColumn(z3Context,node.getType());
            SymbolicUserDefun symbolicUserDefun = new SymbolicUserDefun(funName,args,this.output);
            oldFuns.add(symbolicUserDefun);
            registerFunction.put(funName,oldFuns);
        }
    }
}
