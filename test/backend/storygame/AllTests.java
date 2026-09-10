package backend.storygame;

public final class AllTests {
    private AllTests() {
    }

    public static void main(String[] arguments) throws Exception {
        JsonTest.run();
        GameEngineTest.run();
        GameServerTest.run();
        System.out.println("All Storyweave tests passed.");
    }
}