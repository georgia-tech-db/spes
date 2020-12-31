package AlgeNode;

import com.microsoft.z3.Context;

import java.util.ArrayList;
import java.util.HashSet;

public class EmptyNode extends AlgeNode{
    public EmptyNode(Context z3Context){
        setBasicFields(z3Context,new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new HashSet<>());
    }

    public boolean constructQPSR(AlgeNode node) {
        return (node instanceof EmptyNode);
    }

    public AlgeNode clone(){
        return (new EmptyNode(this.z3Context));
    }

    @Override
    public String toString() {
        return "EmptyTable";
    }
}
