package AlgeRule;

import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlKind;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import AlgeNode.AlgeNode;
import AlgeNode.SPJNode;
import AlgeNode.TableNode;
import RexNodeHelper.RexNodeHelper;
import SymbolicRexNode.BoolPredicate;
import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JoinToProject extends AlgeRuleBase{
  static Map <String, Integer> keys;

  private Map<String, Set<Integer>> duplicateIndexes;
  static public int checkKeyIndex(String name){
    if (keys == null){
      keys = new HashMap<>();
      keys.put("EMP", 0);
      keys.put("DEPT", 0);
    }
    if (keys.containsKey(name)){
      return keys.get(name);
    }else{
      return -1;
    }
  }
  public JoinToProject(AlgeNode input){
    this.input = input;
  }
  public boolean preCondition() {
    if (this.input instanceof SPJNode){
      SPJNode spjNode = (SPJNode) this.input;
      List<AlgeNode> inputs = spjNode.getInputs();
      calculateDuplicateSet(inputs);
//      System.out.println("it is here");
//      System.out.println(this.input.toString());
      return !this.duplicateIndexes.isEmpty();
    }
    return false;
  }

  private void calculateDuplicateSet(List<AlgeNode> inputs){
    Map<String, Set<Integer>> nameToTableIndex = new HashMap<>();
    int tableIndex = 0;
    for (AlgeNode input : inputs){
      if (input instanceof TableNode){
        TableNode tableNode = (TableNode) input;
        String tableName = tableNode.getName();
        if (nameToTableIndex.containsKey(tableName)){
          Set<Integer> indexes = nameToTableIndex.get(tableName);
          indexes.add(tableIndex);
        }else{
          Set<Integer> indexes = new HashSet<>();
          indexes.add(tableIndex);
          nameToTableIndex.put(tableName,indexes);
        }
      }
      tableIndex++;
    }
//    System.out.println("first result");
//    System.out.println(nameToTableIndex);
    this.duplicateIndexes = new HashMap<>();
    for (Map.Entry<String, Set<Integer>> tableIndexes : nameToTableIndex.entrySet()){
      String tableName = tableIndexes.getKey();
      Set<Integer> duplicateIndexes = verifyDuplicateJoin(tableName, tableIndexes.getValue());
//      System.out.println("duplicate index:");
//      System.out.println(duplicateIndexes);
      if (duplicateIndexes.size() > 1){
        this.duplicateIndexes.put(tableName, duplicateIndexes);
      }
    }
  }

  private Set<Integer> verifyDuplicateJoin(String tableName, Set<Integer> tableIndexes){
     SPJNode spjNode = (SPJNode) this.input;
     Set<RexNode> conditions = spjNode.getConditions();
     List<Integer> startIndex = new ArrayList<>();
     int count = 0;
     for (AlgeNode input : spjNode.getInputs()){
       startIndex.add(count);
       count = count + input.getOutputExpr().size();
     }
     Set<Integer> duplicateIndex = new HashSet<>();
     int columnIndex = checkKeyIndex(tableName);
     if (columnIndex == -1){
       return new HashSet<>();
     }
     for (Integer i1 : tableIndexes){
       for (Integer i2 : tableIndexes){
         if (!duplicateIndex.contains(i1) || !duplicateIndex.contains(i2)){
           int columnIndex1 = startIndex.get(i1) + columnIndex;
           int columnIndex2 = startIndex.get(i2) + columnIndex;
           if (checkEquivalent(columnIndex1, columnIndex2, conditions)){
             duplicateIndex.add(i1);
             duplicateIndex.add(i2);
           }
         }
       }
     }
     return duplicateIndex;
  }

  static class EqHelper extends RexVisitorImpl<Void> {
    boolean isEq = false;
    int index1;
    int index2;
    protected EqHelper (int index1, int index2) {
      super(false);
      this.index1 = index1;
      this.index2 = index2;
    }

    public boolean isEq(){
      return isEq;
    }

    @Override public Void visitCall(RexCall call) {
      if (call.getKind().equals(SqlKind.EQUALS)){
        RexNode left = call.getOperands().get(0);
        RexNode right = call.getOperands().get(1);
        if ((left instanceof RexInputRef) && (right instanceof RexInputRef)){
          int leftIndex = ((RexInputRef)left).getIndex();
          int rightIndex = ((RexInputRef)right).getIndex();
          isEq = (leftIndex == index1 && rightIndex == index2) || (rightIndex == index1 && leftIndex == index2);
        }
      }else {
        for (RexNode operand : call.operands) {
          operand.accept(this);
        }
      }
      return null;
    }

  }

  private boolean checkEquivalent(int index1, int index2, Set<RexNode> conditions){
    List<SymbolicColumn> symbolicColumns = new ArrayList<>();
    Context z3Context = this.input.getZ3Context();
    for (int i=0;i<this.input.getInputs().size();i++){
      AlgeNode input = this.input.getInputs().get(i);
      for (RexNode inputColumn : input.getOutputExpr()){
        symbolicColumns.add(SymbolicColumn.mkNewSymbolicColumn(z3Context, inputColumn.getType()));
      }
    }
    List<BoolExpr> assign = new ArrayList<>();
    SymbolicColumn symbolicCondition = BoolPredicate.getAndNodeSymbolicColumn(conditions, symbolicColumns ,assign,z3Context);
    BoolExpr conditionHold = symbolicCondition.isValueTrue();
    List<SymbolicColumn> column1 = new ArrayList<>();
    column1.add(symbolicColumns.get(index1));
    List<SymbolicColumn> column2 = new ArrayList<>();
    column2.add(symbolicColumns.get(index2));
    return z3Utility.symbolicOutputEqual(conditionHold, column1, column2, z3Context);
  }

  public AlgeNode transformation() {
//    System.out.println("what is repeat");
//    System.out.println(this.duplicateIndexes);
//    System.out.println("something needs to be done");
    Map<String, Integer> minTableIndex = new HashMap<>();
    for (Map.Entry<String, Set<Integer>> duplicateIndex : this.duplicateIndexes.entrySet()){
      minTableIndex.put(duplicateIndex.getKey(),getMin(duplicateIndex.getValue()));
    }
    List<AlgeNode> newInputs = new ArrayList<AlgeNode>();
    int tableIndex = 0;
    Map<Integer, Integer> columnIndexSub = new HashMap<>();
    List<Integer> removedTable = new ArrayList<>();
    List<Integer> newInputStartIndex = new ArrayList<>();
    int removedInputs = 0;
    int startIndex = 0;
    for (AlgeNode input : this.input.getInputs()){
      removedTable.add(removedInputs);
      newInputStartIndex.add(startIndex);
      if (isRemove(input, tableIndex)){
        removedInputs++;
        buildMap(tableIndex,removedTable, newInputStartIndex, columnIndexSub);
      }else{
        buildMap(tableIndex, startIndex, columnIndexSub);
        startIndex = startIndex + input.getOutputExpr().size();
        newInputs.add(input);
      }
      tableIndex++;
    }
    List<RexNode> newOutputExprs = new ArrayList<>();
    for (RexNode outputExpr : this.input.getOutputExpr()){
      newOutputExprs.add(RexNodeHelper.substitute(outputExpr,columnIndexSub));
    }
    Set<RexNode> newConditions = new HashSet<>();
    for (RexNode condition : this.input.getConditions()){
      newConditions.add(RexNodeHelper.substitute(condition, columnIndexSub));
    }
//    System.out.println("after transformation");
//    System.out.println(new SPJNode(newOutputExprs, newConditions, newInputs, this.input.getZ3Context()));
    return new SPJNode(newOutputExprs, newConditions, newInputs, this.input.getZ3Context());
  }

  private void buildMap(int tableIndex, int startIndex, Map<Integer, Integer> columnIndexSub){
    int oldStartIndex = getOldStartIndex(tableIndex);
    int bound = this.input.getInputs().get(tableIndex).getOutputExpr().size();
    for (int i=0; i < bound; i++){
      columnIndexSub.put(oldStartIndex+i, startIndex+i);
    }
  }

  // TODO
  private void buildMap(int removeIndex, List<Integer> tableOffSet, List<Integer> startIndexes, Map<Integer, Integer> columnIndexSub){
    TableNode removeTable = (TableNode) this.input.getInputs().get(removeIndex);
//    System.out.println("show again" + this.duplicateIndexes);
//    System.out.println("table name:"+removeTable.getName());
//    System.out.println();
    int minIndex = this.getMin(this.duplicateIndexes.get(removeTable.getName()));
//    System.out.println("minIndex:"+minIndex);
//    System.out.println("tableOffSet:"+tableOffSet.get(minIndex));
    int currentTableIndex = minIndex - tableOffSet.get(minIndex);
    int startIndex = startIndexes.get(currentTableIndex);
    int oldStartIndex = getOldStartIndex(removeIndex);
    for (int i=0; i < removeTable.getOutputExpr().size(); i++){
      columnIndexSub.put(oldStartIndex+i, startIndex+i);
    }
  }

  private int getOldStartIndex(int removeIndex){
    int count = 0;
    for (int i=0; i<removeIndex; i++){
      count = count + this.input.getInputs().get(i).getOutputExpr().size();
    }
    return count;
  }


  private boolean isRemove(AlgeNode input, int tableIndex){
    if (input instanceof TableNode){
      TableNode tableNode = (TableNode) input;
      if (this.duplicateIndexes.containsKey(tableNode.getName())){
        Set<Integer> copyIndex = this.duplicateIndexes.get(tableNode.getName());
        if (copyIndex.contains(tableIndex)){
          int minIndex = getMin(copyIndex);
          if (minIndex != tableIndex){
            return true;
          }
        }
      }
    }
    return false;
  }

  private int getMin (Set<Integer> indexes){
    int min = Integer.MAX_VALUE;
    for (Integer index : indexes){
      int value = index.intValue();
      if ( value < min){
        min = value;
      }
    }
    return min;
  }
}
