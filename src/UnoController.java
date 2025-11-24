import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * The UnoController connects the UnoModel, UnoView, and UnoFrame.
 * It handles all user interactions (button presses and card selections)
 * and updates both the model and the view accordingly.
 */

public class UnoController implements ActionListener {
    /** The game model holding players, decks, and game logic. */
    private final UnoModel model;

    /** The view responsible for rendering the state of the game. */
    private final UnoView view;

    /** The main game window with GUI components. */
    private final UnoFrame frame;

    /** Flag indicating if the current player's turn has already advanced. */
    private boolean isAdvanced;

    /** Guard flag to avoid recursively entering the AI turn loop. */
    private boolean handlingAITurn;

    /** Holds the last status message from an AI action so it can be shown alongside turn handoff text. */
    private String pendingAIStatusMessage;

    private void updateStatusWithPending(String status) {
        if (pendingAIStatusMessage != null && handlingAITurn) {
            status = pendingAIStatusMessage + " " + status;
            pendingAIStatusMessage = null;
        }
        view.updateStatusMessage(status);
    }

    /**
     * Constructs the controller with references to the model, view, and frame.
     *
     * @param model game model containing the core logic
     * @param view  view interface for displaying updates
     * @param frame main application window
     */
    public UnoController(UnoModel model, UnoView view, UnoFrame frame) {
        this.model = model;
        this.view = view;
        this.frame = frame;
        isAdvanced = false;
        handlingAITurn = false;
        pendingAIStatusMessage = null;
    }

    private String buildActionCommand(Card card) {
        if (card.getValue().equals(UnoModel.Values.WILD) || card.getValue().equals(UnoModel.Values.WILD_DRAW_TWO)) {
            return card.getValue() + "_" + System.identityHashCode(card);
        }
        return card.getColour() + "_" + card.getValue();
    }

    private UnoModel.Colours chooseColourForAI() {
        Player player = model.getCurrPlayer();
        int[] counts = new int[UnoModel.Colours.values().length];
        for (Card card : player.getPersonalDeck()) {
            UnoModel.Colours colour = card.getColour();
            if (colour != null) {
                counts[colour.ordinal()]++;
            }
        }
        return UnoModel.Colours.values()[indexOfMax(counts)];
    }

    private UnoModel.ColoursDark chooseDarkColourForAI() {
        Player player = model.getCurrPlayer();
        int[] counts = new int[UnoModel.ColoursDark.values().length];
        for (Card card : player.getPersonalDeck()) {
            UnoModel.ColoursDark colour = card.getColourDark();
            if (colour != null) {
                counts[colour.ordinal()]++;
            }
        }
        return UnoModel.ColoursDark.values()[indexOfMax(counts)];
    }

    private int indexOfMax(int[] values) {
        int bestIndex = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Starts the game by:
     * - Adding players from the frame
     * - Dealing initial hands
     * - Enabling the first player's actions
     */
    public void play() {
        // Add players from the frame's setup
        List<Boolean> aiPlayers = frame.getAiPlayers();
        List<String> playerNames = frame.getPlayerName();

        for (int i = 0; i < playerNames.size(); i++) {
            String player = playerNames.get(i);
            boolean isAI = aiPlayers.get(i);
            model.addPlayer(player, isAI);
        }

        // Start a new round (deal cards, choose initial top card)
        model.newRound();

        // Update the initial hand display and enable cards
        view.updateHandPanel(model, this);
        frame.enableCards();

        // Update initial status message
        view.updateStatusMessage("Game started. It is " + model.getCurrPlayer().getName() + "'s turn.");

        maybeRunAITurn();
    }

    /**
     * Handles button clicks and card selections:
     * - "Next Player" button: advances to next player, checks round/match end.
     * - "Draw Card" button: draws a new card for the current player.
     * - Card buttons: attempts to play the selected card if legal, and applies card effects.
     *
     * @param e the action event triggered by user interaction
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        // Handle "Next Player" button presses
        if (e.getActionCommand().equals("Next Player")) {
            if (!isAdvanced) {
                model.advance();
            }

            // Round end: current player has no cards
            if (model.isDeckEmpty()) {
                Player winner = model.getCurrPlayer();
                int score = model.getScore(winner);

                view.updateStatusMessage("Round over: " + winner.getName() + " wins this round and gets " + score + " points.");
                view.updateWinner(winner.getName(), score);

                boolean matchOver = model.checkWinner(winner);

                if (matchOver) {
                    view.updateStatusMessage("Game over: " + winner.getName() + " wins the game, reaching 500 or more points.");
                    frame.disableAllButtons();
                    return;
                }

                String option = frame.newRoundSelectionDialog();
                if (option == null || option.equals("Quit")) {
                    System.exit(0);
                } else {
                    model.newRound();
                    view.updateHandPanel(model, this);
                    frame.enableCards();
                    view.updateStatusMessage("New round started. It is " + model.getCurrPlayer().getName() + "'s turn.");
                    maybeRunAITurn();
                }
            } else {
                view.updateHandPanel(model, this);
                frame.enableCards();
                isAdvanced = false;
                updateStatusWithPending("Turn passed to " + model.getCurrPlayer().getName() + ".");
            }

            maybeRunAITurn();
        }

        // Handle "Draw Card" button presses
        else if (e.getActionCommand().equals("Draw Card")) {
            boolean canDraw = false;
            for (Card card : model.getCurrPlayer().getPersonalDeck()) {
                if (model.isPlayable(card)) {
                    canDraw = true;
                    break;
                }
            }

            if (!canDraw) {
                model.drawCard();
                view.updateHandPanel(model, this);
                frame.disableCardButtons();
                view.updateStatusMessage(model.getCurrPlayer().getName() + " draws one card.");
                isAdvanced = false;
            } else {
                view.updateStatusMessage("You have a card that can be played. Please play it instead of drawing.");
            }
        }

        // Handle card selections
        else {
            Card cardPicked = null;
            String cmd;

            // Identify which card was clicked by matching command strings
            for (Card card : model.getCurrPlayer().getPersonalDeck()) { //Find the card that was picked
                if (card.getValue().equals(UnoModel.Values.WILD) || card.getValue().equals(UnoModel.Values.WILD_DRAW_TWO)) {
                    cmd = card.getValue() + "_" + System.identityHashCode(card);        // Unique per instance
                } else {
                    cmd = card.getColour() + "_" + card.getValue();

                }
                if (cmd.equals(e.getActionCommand())) {
                    cardPicked = card;
                    break;
                }
            }

            if (cardPicked != null && model.isPlayable(cardPicked)) {
                model.playCard(cardPicked);

                if (model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.DRAW_ONE)) {
                    model.drawOne();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage("Next player draws one card.");
                }

                else if (model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.REVERSE)) {
                    model.reverse();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage("Play direction reversed.");
                }

                else if (model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.SKIP)) {
                    Player skippedPlayer = model.getNextPlayer();
                    model.skip();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;
                    view.updateStatusMessage(skippedPlayer.getName() + " is skipped. Turn passes to " +
                            model.getCurrPlayer().getName() + ".");
                }

                else if (model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.WILD)) {
                    String colour;
                    if (model.getCurrPlayer().isAI()) {
                        UnoModel.Colours chosen = chooseColourForAI();
                        model.wild(chosen);
                        colour = chosen.toString();
                    } else {
                        colour = frame.colourSelectionDialog();
                        if (colour != null) {
                            model.wild(UnoModel.Colours.valueOf(colour));
                        }
                    }
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    if (colour != null) {
                        String status = "New colour chosen, " + colour + ".";
                        if (model.getCurrPlayer().isAI()) {
                            pendingAIStatusMessage = status;
                        }
                        view.updateStatusMessage(status);
                    } else {
                        view.updateStatusMessage("Wild colour chosen by AI.");
                    }
                }

                else if (model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.WILD_DRAW_TWO)) {
                    String colour;
                    String nextPlayer = model.getNextPlayer().getName();
                    if (model.getCurrPlayer().isAI()) {
                        UnoModel.Colours chosen = chooseColourForAI();
                        model.wildDrawTwo(chosen);
                        colour = chosen.toString();
                    } else {
                        colour = frame.colourSelectionDialog();
                        if (colour != null) {
                            model.wildDrawTwo(UnoModel.Colours.valueOf(colour));    // Next player draws 2 + skip
                        }
                    }

                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;                                          // Turn skip already applied
                    if (colour != null) {
                        String status = "New colour chosen, " + colour + ", " + nextPlayer + " draws two cards and skips their turn.";
                        if (model.getCurrPlayer().isAI()) {
                            pendingAIStatusMessage = status;
                        }
                        view.updateStatusMessage(status);
                    } else {
                        view.updateStatusMessage(nextPlayer + " draws two cards and skips their turn.");
                    }
                    maybeRunAITurn();
                    return;
                }

                else if (cardPicked.getValueDark().equals(UnoModel.ValuesDark.DRAW_FIVE)) {
                    model.drawFive();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;
                    String nextPlayer = model.getNextPlayer().getName();
                    view.updateStatusMessage(nextPlayer + " draws 5 cards and loses their turn.");
                    maybeRunAITurn();
                    return;
                }

                else if (cardPicked.getValueDark().equals(UnoModel.ValuesDark.SKIP_ALL)) {
                    model.skipAll();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage("All players skipped. Turn returns to " + model.getCurrPlayer().getName() + ".");
                }

                else if (cardPicked.getValueDark().equals(UnoModel.ValuesDark.WILD_STACK)) {
                    String colour;
                    if (model.getCurrPlayer().isAI()) {
                        UnoModel.ColoursDark chosen = chooseDarkColourForAI();
                        model.setInitWildStack(chosen);
                        colour = chosen.toString();
                    } else {
                        colour = frame.colourSelectionDialogDark(); // Choose new colour
                        if (colour != null) {
                            model.setInitWildStack(UnoModel.ColoursDark.valueOf(colour));
                        }
                    }
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    String nextPlayer = model.getNextPlayer().getName();
                    if (colour != null) {
                        view.updateStatusMessage("New colour chosen, " + colour + ", " + nextPlayer +
                                " keeps drawing cards until a " + colour + " card is chosen.");
                    } else {
                        view.updateStatusMessage(nextPlayer +
                                " keeps drawing cards until the chosen colour is drawn.");
                    }
                }

                else if (cardPicked.getValueDark().equals(UnoModel.ValuesDark.FLIP) || cardPicked.getValue().equals(UnoModel.Values.FLIP)) {
                    model.flip();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage("Deck flipped to " + model.getSide() + " side.");
                }

                else {
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage(model.getCurrPlayer().getName() + " played a card.");
                }

                // Check if the current player has emptied their hand after playing
                if (model.isDeckEmpty()) {
                    Player winner = model.getCurrPlayer();
                    int score = model.getScore(winner);

                    view.updateStatusMessage("Round over: " + winner.getName() + " wins this round and gets " + score + " points.");
                    view.updateWinner(winner.getName(), score);

                    boolean matchOver = model.checkWinner(winner);

                    if (matchOver) {
                        view.updateStatusMessage("Game over: " + winner.getName() + " wins the game, reaching 500 or more points.");
                        frame.disableAllButtons();
                        return;
                    } else {
                        String option = frame.newRoundSelectionDialog();
                        if (option == null || option.equals("Quit")) {
                            System.exit(0);
                        } else {
                            model.newRound();
                            view.updateHandPanel(model, this);
                            frame.enableCards();
                            isAdvanced = false;
                            view.updateStatusMessage("New round started. It is " + model.getCurrPlayer().getName() + "'s turn.");
                            maybeRunAITurn();
                        }
                    }
                }
            }
            // Invalid move feedback
            if (cardPicked != null && !model.isPlayable(cardPicked)) {
                view.updateStatusMessage("Placing that card is not a valid move. Try again.");
            }
        }
    }

    private void maybeRunAITurn() {
        if (handlingAITurn) {
            return;
        }

        handlingAITurn = true;
        try {
            while (model.getCurrPlayer().isAI()) {
                frame.disableAllButtons();
                view.updateStatusMessage(model.getCurrPlayer().getName() + " (AI) is playing.");

                Card aiChoice = model.chooseAICardForCurrPlayer();

                if (aiChoice != null) {
                    actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, buildActionCommand(aiChoice)));
                } else {
                    actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Draw Card"));
                }

                actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Next Player"));
            }
        } finally {
            handlingAITurn = false;
        }
    }
}
