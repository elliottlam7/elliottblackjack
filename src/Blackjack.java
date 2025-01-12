// Name ~ Elliott Lam
// Class ~ Y11 Computer Science: Section 14
// Date of Submission ~ Jan 12, 2025
// Description ~ This is the Java code for my Blackjack game.



import java.io.*;
import java.util.*;

public class Blackjack {

    private static final int INITIAL_BALANCE = 100;
    private static final int DEALER_STAND_THRESHOLD = 17;
    private static final int MIN_BET = 10;
    private static final String STATS_FILE = "playerStats.properties";

    private static List<Integer> deck = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);

    private static class Player {
        private long balance;
        private int gamesWon;
        private int gamesLost;
        private int gamesTied;

        public Player(long initialBalance) {
            this.balance = initialBalance;
            this.gamesWon = 0;
            this.gamesLost = 0;
            this.gamesTied = 0;
        }

        public long getBalance() {
            return balance;
        }

        public void updateBalance(long amount) {
            this.balance += amount;
            saveStats();
        }

        public void incrementGamesWon() {
            gamesWon++;
            saveStats();
        }

        public void incrementGamesLost() {
            gamesLost++;
            saveStats();
        }

        public void incrementGamesTied() {
            gamesTied++;
            saveStats();
        }

        public void resetStats() {
            balance = INITIAL_BALANCE;
            gamesWon = 0;
            gamesLost = 0;
            gamesTied = 0;
            saveStats();
        }

        public void saveStats() {
            try (FileWriter writer = new FileWriter(STATS_FILE)) {
                Properties props = new Properties();
                props.setProperty("balance", String.valueOf(balance));
                props.setProperty("gamesWon", String.valueOf(gamesWon));
                props.setProperty("gamesLost", String.valueOf(gamesLost));
                props.setProperty("gamesTied", String.valueOf(gamesTied));
                props.store(writer, "Player Statistics");
            } catch (IOException e) {
                System.out.println("Error saving stats: " + e.getMessage());
            }
        }

        public void loadStats() {
            try (FileReader reader = new FileReader(STATS_FILE)) {
                Properties props = new Properties();
                props.load(reader);
                balance = Long.parseLong(props.getProperty("balance", String.valueOf(INITIAL_BALANCE)));
                gamesWon = Integer.parseInt(props.getProperty("gamesWon", "0"));
                gamesLost = Integer.parseInt(props.getProperty("gamesLost", "0"));
                gamesTied = Integer.parseInt(props.getProperty("gamesTied", "0"));
            } catch (IOException e) {
                System.out.println("No stats found or error loading stats. Starting fresh.");
                resetStats();
            }
        }
    }

    public static void main(String[] args) {
        Player player = new Player(INITIAL_BALANCE);

        System.out.print("Would you like to continue your previous game? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();
        if (response.equals("yes")) {
            player.loadStats();
            if (player.getBalance() < MIN_BET) {
                System.out.println("Insufficient balance. Resetting stats.");
                player.resetStats();
            }
        } else {
            player.resetStats();
        }

        System.out.println("Welcome to Elliott's Blackjack!");
        while (player.getBalance() >= MIN_BET) {
            System.out.println("\nCurrent Balance: " + player.getBalance());
            int bet = placeBet(player);

            initDeck();
            List<Integer> playerHand = dealInitialHand();
            List<Integer> dealerHand = dealInitialHand();

            System.out.println("Your cards: " + playerHand + " (Total: " + calculateHandValue(playerHand) + ")");
            System.out.println("Dealer's visible card: " + dealerHand.get(0));

            // Surrender
            if (promptYesNo("Would you like to surrender? (yes/no): ")) {
                System.out.println("You surrendered. You lose half your bet.");
                player.updateBalance(-(bet / 2));
                player.incrementGamesLost();
                continue;
            }

            // Double down
            boolean doubledDown = false;
            if (player.getBalance() >= bet && promptYesNo("Would you like to double down? (yes/no): ")) {
                bet *= 2;
                doubledDown = true;
                playerHand.add(dealCard());
                System.out.println("Your new card: " + playerHand.get(playerHand.size() - 1));
                System.out.println("Your cards: " + playerHand + " (Total: " + calculateHandValue(playerHand) + ")");
            }

            // Split
            if (canSplit(playerHand) && player.getBalance() >= bet) {
                if (promptYesNo("You have a pair! Would you like to split? (yes/no): ")) {
                    List<Integer> splitHand1 = new ArrayList<>(Arrays.asList(playerHand.get(0), dealCard()));
                    List<Integer> splitHand2 = new ArrayList<>(Arrays.asList(playerHand.get(1), dealCard()));
                    System.out.println("Playing split hands...");
                    playSplitHands(player, splitHand1, splitHand2, dealerHand, bet);
                    continue;
                }
            }

            if (!doubledDown) {
                playTurn(playerHand, false);
            }

            if (calculateHandValue(playerHand) <= 21) {
                playTurn(dealerHand, true);
                determineWinner(player, playerHand, dealerHand, bet);
            } else {
                System.out.println("You bust! You lose " + bet);
                player.updateBalance(-bet);
                player.incrementGamesLost();
            }

            endRound(player);
        }

        System.out.println("Game over. You have no money left.");
        player.saveStats();
    }

    private static void playSplitHands(Player player, List<Integer> hand1, List<Integer> hand2, List<Integer> dealerHand, int bet) {
        System.out.println("Playing Hand 1:");
        System.out.println("Your cards: " + hand1 + " (Total: " + calculateHandValue(hand1) + ")");
        playTurn(hand1, false);

        if (calculateHandValue(hand1) <= 21) {
            playTurn(dealerHand, true);
            determineWinner(player, hand1, dealerHand, bet);
        } else {
            System.out.println("Hand 1 busts. You lose " + bet);
            player.updateBalance(-bet);
            player.incrementGamesLost();
        }

        initDeck();
        dealerHand = dealInitialHand();

        System.out.println("Playing Hand 2:");
        System.out.println("Your cards: " + hand2 + " (Total: " + calculateHandValue(hand2) + ")");
        playTurn(hand2, false);

        if (calculateHandValue(hand2) <= 21) {
            playTurn(dealerHand, true);
            determineWinner(player, hand2, dealerHand, bet);
        } else {
            System.out.println("Hand 2 busts. You lose " + bet);
            player.updateBalance(-bet);
            player.incrementGamesLost();
        }
    }

    private static int placeBet(Player player) {
        while (true) {
            int bet = Integer.parseInt(getValidatedInput("Enter your bet amount (minimum " + MIN_BET + "): ", "\\d+"));
            if (bet >= MIN_BET && bet <= player.getBalance()) {
                return bet;
            }
            System.out.println("Invalid bet amount. Must be between " + MIN_BET + " and your current balance.");
        }
    }

    private static void initDeck() {
        deck.clear();
        for (int i = 1; i <= 10; i++) {
            deck.addAll(Collections.nCopies(4, i));
        }
        deck.addAll(Collections.nCopies(12, 10));
        Collections.shuffle(deck);
    }

    private static List<Integer> dealInitialHand() {
        return new ArrayList<>(Arrays.asList(dealCard(), dealCard()));
    }

    private static int dealCard() {
        return deck.remove(deck.size() - 1);
    }

    private static void playTurn(List<Integer> hand, boolean isDealer) {
        while (calculateHandValue(hand) < (isDealer ? DEALER_STAND_THRESHOLD : 21)) {
            if (!isDealer) {
                String action = getValidatedInput("Hit or Stand? (hit/stand): ", "hit|stand");
                if (action.equals("stand")) break;
            }
            hand.add(dealCard());
            if (isDealer) {
                System.out.println("Dealer's hand: " + hand);
            } else {
                System.out.println("Your cards: " + hand);
            }
        }
    }

    private static void determineWinner(Player player, List<Integer> playerHand, List<Integer> dealerHand, int bet) {
        int playerTotal = calculateHandValue(playerHand);
        int dealerTotal = calculateHandValue(dealerHand);

        System.out.println("Dealer's cards: " + dealerHand + " (Total: " + dealerTotal + ")");
        if (dealerTotal > 21 || playerTotal > dealerTotal) {
            System.out.println("You win! You gain " + bet);
            player.updateBalance(bet);
            player.incrementGamesWon();
        } else if (playerTotal == dealerTotal) {
            System.out.println("It's a tie! No money lost or gained.");
            player.incrementGamesTied();
        } else {
            System.out.println("You lose! You lose " + bet);
            player.updateBalance(-bet);
            player.incrementGamesLost();
        }
    }

    private static void endRound(Player player) {
        System.out.println("Round over. Current balance: " + player.getBalance());
        if (player.getBalance() < MIN_BET) {
            System.out.println("Balance too low for another round.");
        }
    }

    private static int calculateHandValue(List<Integer> hand) {
        int value = 0;
        int aceCount = 0;

        for (int card : hand) {
            if (card > 10) {
                value += 10;
            } else if (card == 1) {
                aceCount++;
                value += 11;
            } else {
                value += card;
            }
        }

        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }
        return value;
    }

    private static String getValidatedInput(String prompt, String regex) {
        String input;
        while (true) {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (input.toLowerCase().matches(regex)) {
                return input.toLowerCase();
            }
            System.out.println("Invalid input. Please try again.");
        }
    }

    private static boolean promptYesNo(String prompt) {
        String response = getValidatedInput(prompt, "yes|no");
        return response.equals("yes");
    }

    private static boolean canSplit(List<Integer> hand) {
        return hand.size() == 2 && hand.get(0).equals(hand.get(1));
    }
}