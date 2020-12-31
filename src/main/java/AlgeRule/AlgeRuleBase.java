package AlgeRule;

import AlgeNode.AlgeNode;

public abstract class AlgeRuleBase {
    protected AlgeNode input;
    public abstract boolean  preCondition();
    public abstract AlgeNode transformation();
}
