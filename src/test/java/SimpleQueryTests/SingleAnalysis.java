package SimpleQueryTests;

import AlgeNode.AlgeNode;
import AlgeNodeParser.AlgeNodeParserPair;
import AlgeRule.AlgeRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.z3.Context;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlExplainLevel;

import java.io.*;

public class SingleAnalysis {
    static long time = 0;
    public static boolean BeVerified(String sql1, String sql2, String name, PrintWriter cannotCompile, PrintWriter cannotProve, PrintWriter prove)
      throws Exception {
        if((contains(sql1))|| (contains(sql2))) {
            System.out.println("reason 1");
            return false;
        }
        if((notUnionAll(sql1))|| (notUnionAll(sql2))) {
            System.out.println("reason 2");
            return false;
        }
        System.out.println("it is here");
            simpleParser parser = new simpleParser();
            simpleParser parser2 = new simpleParser();
            RelNode logicPlan = parser.getRelNode(sql1);
            RelNode logicPlan2 = parser2.getRelNode(sql2);
            System.out.println(RelOptUtil.toString(logicPlan));
            System.out.println(RelOptUtil.toString(logicPlan2));
            Context z3Context = new Context();
            AlgeNode algeExpr = AlgeRule.normalize(AlgeNodeParserPair.constructAlgeNode(logicPlan,z3Context));
            AlgeNode algeExpr2 = AlgeRule.normalize(AlgeNodeParserPair.constructAlgeNode(logicPlan2,z3Context));
            System.out.println("this is here");
            System.out.println(algeExpr);
            System.out.println(algeExpr2);
                long startTime = System.currentTimeMillis();
                if(algeExpr.isEq(algeExpr2)){
                    long stopTime = System.currentTimeMillis();
                    time =(stopTime - startTime)+time;
                    //System.out.println("time:"+time);
                    System.out.println(RelOptUtil.toString(logicPlan,SqlExplainLevel.EXPPLAN_ATTRIBUTES));
                    System.out.println(RelOptUtil.toString(logicPlan2,SqlExplainLevel.EXPPLAN_ATTRIBUTES));
                    prove.println(name);
                    prove.flush();
                    z3Context.close();
                    System.out.println("They are equivalent");
                    return true;
                }else{
                    //System.out.println("it fails");
                    //System.out.println(name);
                    //System.out.println(RelOptUtil.toString(logicPlan,SqlExplainLevel.EXPPLAN_ATTRIBUTES));
                    //System.out.println(RelOptUtil.toString(logicPlan2,SqlExplainLevel.EXPPLAN_ATTRIBUTES));
                    cannotProve.println(name);
                    cannotProve.flush();
                    z3Context.close();
                    System.out.println("We don't know");
                    return false;
                }
    }
    public static boolean simpleVerify(String sql1, String sql2){
        RelNode logicPlan = null;
        RelNode logicPlan2 = null;
        try {
            simpleParser parser = new simpleParser();
            logicPlan = parser.getRelNode(sql1);
            simpleParser parser2 = new simpleParser();
            logicPlan2 = parser2.getRelNode(sql2);
        }catch (Exception e){
            System.out.println(e);
        }
        Context z3Context = new Context();
        AlgeNode algeExpr = AlgeNodeParserPair.constructAlgeNode(logicPlan,z3Context);
        AlgeNode algeExpr2 = AlgeNodeParserPair.constructAlgeNode(logicPlan2,z3Context);
        System.out.println(algeExpr);
        System.out.println(algeExpr2);
        return algeExpr.isEq(algeExpr2);
    }
    public static boolean simplify(String sql1, String sql2){
        RelNode logicPlan = null;
        RelNode logicPlan2 = null;
        try {
            simpleParser parser = new simpleParser();
            logicPlan = parser.getRelNode(sql1);
            simpleParser parser2 = new simpleParser();
            logicPlan2 = parser2.getRelNode(sql2);
        }catch (Exception e){
            System.out.println(e);
        }
        Context z3Context = new Context();
        AlgeNode algeExpr =AlgeRule.normalize(AlgeNodeParserPair.constructAlgeNode(logicPlan,z3Context));
        AlgeNode algeExpr2 =AlgeRule.normalize(AlgeNodeParserPair.constructAlgeNode(logicPlan2,z3Context));
        System.out.println(algeExpr);
        System.out.println(algeExpr2);
        return false;
    }
    public static void main(String[] args) throws Exception {
        File f = new File("testData/calcite_tests.json");
        JsonParser parser = new JsonParser();
        JsonArray array = parser.parse(new FileReader(f)).getAsJsonArray();
        FileWriter prove = new FileWriter("calciteProve.txt");
        BufferedWriter bw = new BufferedWriter(prove);
        PrintWriter out = new PrintWriter(bw);
        FileWriter notProve = new FileWriter("tryToHandle.txt");
        BufferedWriter bw2 = new BufferedWriter(notProve);
        PrintWriter out2 = new PrintWriter(bw2);
        FileWriter notCompile = new FileWriter("cannotCompile.txt");
        BufferedWriter bw3 = new BufferedWriter(notCompile);
        PrintWriter out3 = new PrintWriter(bw3);
        int count = 0;
        for(int i=0;i<array.size();i++){
            JsonObject testCase = array.get(i).getAsJsonObject();
            String query1 = testCase.get("q1").getAsString();
            String query2 = testCase.get("q2").getAsString();
            String name = testCase.get("name").getAsString();


            if(!name.equals("testPushSemiJoinPastJoinRuleLeft")){
                continue;
            }else {
                System.out.println("we have this pair");
            }

                boolean result = BeVerified(query1,query2,name,out3,out2,out);
                //boolean result = simpleVerify(query1,query2);
                //boolean result = simplify(query1,query2);
                if(result){
                    count++;
                }

        }
        System.out.println("what is the number:"+count);
        out.close();
        out2.close();
        out3.close();
//        String query1 = load.get("skeynet_id1")+"_"+load.get("instance_id1");
//        String query2 = load.get("skeynet_id2")+"_"+load.get("instance_id2");
//        String sql = "SELECT 1 FROM EMP AS EMP INNER JOIN EMP AS EMP0 ON EMP.DEPTNO = EMP0.DEPTNO;";
//        String sql2 ="SELECT 1 FROM EMP AS EMP1 INNER JOIN EMP AS EMP2 ON EMP1.DEPTNO = EMP2.DEPTNO;";
//        boolean result = BeVerified(sql,sql2);
//        System.out.println(result);
    }
    static public boolean contains(String sql){
        String[] keyWords ={"VALUE","EXISTS","ROW","ORDER","CAST","INTERSECT","EXCEPT"," IN "};
        for (String keyWord : keyWords) {
            if (sql.contains(keyWord)) {
                return true;
            }
        }
        return false;
    }
    static public boolean notUnionAll(String sql){
        if(sql.contains("UNION")&&(!sql.contains("UNION ALL"))){
            return true;
        }
        return false;
    }
}
