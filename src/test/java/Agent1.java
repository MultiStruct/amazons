import java.util.Random;
import java.util.Scanner;

public class Agent1 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        int boardSize = in.nextInt();
        int currentTurn = 1000;
        while (true) {
            String color = in.next();
            if (color.equals("w") && currentTurn >= 1000) {
                currentTurn = 0;
            }

            for (int y = 0; y < boardSize; ++y) {
                String line = in.next();
                System.err.println(line);
            }

            String last_action = in.next();

            int actions = in.nextInt();

            System.out.println("random");

            currentTurn++;
        }
    }
}
