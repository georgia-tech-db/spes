package SimpleQueryTests;

import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.ReduceExpressionsRule;

public class testOftest {
    public static void main(String[] args) throws Exception{
        HepProgram preProgram = new HepProgramBuilder()
                .build();

        HepProgramBuilder builder = new HepProgramBuilder();
        builder.addRuleClass(ReduceExpressionsRule.class);
        HepPlanner hepPlanner = new HepPlanner(builder.build());
        hepPlanner.addRule(ReduceExpressionsRule.FILTER_INSTANCE);

        final String sql = "select sum(sal)\n"
                + "from emp";
        simpleParser parser = new simpleParser();
        RelNode newNode = parser.getRelNode(sql);

    }
}
