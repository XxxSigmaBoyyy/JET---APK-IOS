package android.animation;
public class IntEvaluator implements TypeEvaluator<Integer> { public Integer evaluate(float f, Integer a, Integer b) { return (int) (a + f * (b - a)); } }
