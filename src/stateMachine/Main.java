package stateMachine;
import stateMachine.StateMachine.Graph;

import java.io.FileInputStream;

public class Main {
    public static void main(String[] args) {
        final String fileName = "src/stateMachine/input/graph2.txt";

        try (FileInputStream fis = new FileInputStream(fileName)) {
            Graph graph = StateMachine.Graph.buildGraph(fis);
            graph.determinize(true);
            graph.rename(null);
            graph.printGraph(System.out);
            System.out.println("*******************************");
            graph = StateMachine.Graph.minimize(graph, System.out);
            System.out.println("*******************************");
            graph.printGraph(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
