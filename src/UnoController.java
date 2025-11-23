import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;


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

    /** The main game window containing UI components. */
    private final UnoFrame frame;

    /** Flags indicating which players are AI-controlled. Indexed by player order. */
    private boolean[] aiPlayers;

    /** True while the controller is performing AI turns (prevents recursion). */
    private boolean aiTurnInProgress;

    /**
     * Tracks whether the "Next Player" advance action has already been applied
     * due to a card effect (e.g., Skip or Wild Draw Two). Prevents double-advancing.
     */
    private boolean isAdvanced;


    /**
     * Constructs a controller with the provided model, view, and frame.
     *
     * @param model the game model
     * @param view the user interface view for displaying the game
     * @param frame the top-level game window and UI handler
     */
    public UnoController(UnoModel model, UnoView view, UnoFrame frame) {
        this.model = model;
        this.view = view;
        this.frame = frame;

        isAdvanced = false;
        aiPlayers = null;
        aiTurnInProgress = false;
    }

    /**
     * Starts the game by:
     * - Adding players from the frame
     * - Initializing a new round
     * - Updating the view and hand panel
     * - Enabling card interaction
     */
    public void play() {
        java.util.List<String> names = frame.getPlayerName();
        aiPlayers = new boolean[names.size()];

        for (int i = 0; i < names.size(); i++) {
            String playerName = names.get(i);
            int choice = JOptionPane.showConfirmDialog(
                    null,
                    "Should " + playerName + " be controlled by the computer (AI)?",
                    "Player Type",
                    JOptionPane.YES_NO_OPTION
            );
            aiPlayers[i] = (choice == JOptionPane.YES_OPTION);
            model.addPlayer(playerName);
        }

        model.newRound();
        view.updateHandPanel(model, this);
        frame.enableCards();
        triggerAITurnIfNeeded();
    }


    /**
     * Handles all UI action events such as:
     * - "Next Player" button
     * - "Draw Card" button
     * - Playing a selected card from the player's hand
     *
     * @param e the action event triggered by the UI
     */
    public void actionPerformed(ActionEvent e) {

        // Handle Next Player button
        if(e.getActionCommand().equals("Next Player")) {
            if(!isAdvanced) {
                model.advance();                         // Only advance if no card effect already advanced the turn
            }
            view.updateHandPanel(model, this);

            if(!model.isWildStackCard()) {
                frame.enableCards();
            } else {
                frame.disableCardButtons();
            }

            if (!aiTurnInProgress) {
                triggerAITurnIfNeeded();
            }

        }

        // Handle Draw Card button
        if(e.getActionCommand().equals("Draw Card")) {
            if(model.isWildStackCard()) {
                String drawPlayer = model.getCurrPlayer().getName();
                boolean chosen = model.wildStack();

                view.updateHandPanel(model, this);
                frame.disableCardButtons();

                if(chosen) {
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    view.updateStatusMessage(drawPlayer + " drew the colour");
                    isAdvanced = false;

                } else {
                    view.updateHandPanel(model, this);
                    view.updateStatusMessage("Keep drawing");
                }
            }

            else {
                frame.getNextButton().setEnabled(true);
                model.drawCard();                          // Draw card into player's hand
                isAdvanced = false;
                view.updateHandPanel(model, this);
                frame.disableCards();                          // Disable cards until next turn
                view.updateStatusMessage(model.getCurrPlayer().getName() + " draws a card.");
            }
        }

        // Handle card buttons
        else {
            Card cardPicked = null;
            String cmd;

            // Identify which card was clicked by matching command strings
            for(Card card: model.getCurrPlayer().getPersonalDeck()) { //Find the card that was picked
                if(card.getValue().equals(UnoModel.Values.WILD) || card.getValue().equals(UnoModel.Values.WILD_DRAW_TWO)){
                    cmd = card.getValue() + "_" + System.identityHashCode(card);        // Unique per instance
                } else { cmd = card.getColour() + "_" + card.getValue();

                } if(cmd.equals(e.getActionCommand())) {
                    cardPicked = card;
                    break;
                }
            }

            // If card is valid and playable
            if (cardPicked != null && model.isPlayable(cardPicked)) {

                model.playCard(cardPicked);                 // Apply card to discard pile

                if(model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.DRAW_ONE)){
                    model.drawOne();                         // Next player draws one
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage(model.getNextPlayer().getName() + " draws a card");
                }

                else if(model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.REVERSE)) {
                    model.reverse();                           // Reverse turn order
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage(model.getCurrPlayer().getName() + " has reversed the order");
                }

                else if(model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.SKIP)) {
                    String nextPlayer = model.getNextPlayer().getName();
                    model.skip();                               // Skip next player's turn
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;                           // Skip already advances turn logic
                    view.updateStatusMessage("Skip card has been played, " + nextPlayer + " skips their turn.");
                    return;
                }

                else if(model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.WILD)) {
                    String colour = frame.colourSelectionDialog(); // Choose new colour
                    if(colour != null) {
                        model.wild(UnoModel.Colours.valueOf(colour));
                    }
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage("New colour chosen, " + colour + ".");
                }

                else if(model.getSide() == UnoModel.Side.LIGHT && cardPicked.getValue().equals(UnoModel.Values.WILD_DRAW_TWO)) {
                    String colour = frame.colourSelectionDialog();
                    String nextPlayer = model.getNextPlayer().getName();
                    if(colour != null) {
                        model.wildDrawTwo(UnoModel.Colours.valueOf(colour));    // Next player draws 2 + skip
                    }

                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;                                          // Turn skip already applied
                    view.updateStatusMessage("New colour chosen, " + colour + ", " + nextPlayer + " draws two cards and skips their turn.");
                    return;
                }

                else if(cardPicked.getValueDark().equals(UnoModel.ValuesDark.FLIP) || cardPicked.getValue().equals(UnoModel.Values.FLIP) ) {
                    model.flip();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage("deck has been flipped");
                }

                else if(model.getSide() == UnoModel.Side.DARK && cardPicked.getValueDark().equals(UnoModel.ValuesDark.DRAW_FIVE)) {
                    model.drawFive();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;
                    String nextPlayer = model.getNextPlayer().getName();
                    view.updateStatusMessage("Draw five played and " + nextPlayer + " picks up 5 cards and skips their turn.");
                }

                else if(model.getSide() == UnoModel.Side.DARK && cardPicked.getValueDark().equals(UnoModel.ValuesDark.SKIP_ALL)) {
                    model.skipAll();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;
                    view.updateStatusMessage("Everyone's turn is skipped, " + model.getCurrPlayer().getName() + " plays again." );

                }

                else if(model.getSide() == UnoModel.Side.DARK && cardPicked.getValueDark().equals(UnoModel.ValuesDark.WILD_STACK)) {
                    String colour = frame.colourSelectionDialogDark(); // Choose new colour
                    if(colour != null) {
                        model.setInitWildStack(UnoModel.ColoursDark.valueOf(colour));
                    }
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    String nextPlayer = model.getNextPlayer().getName();
                    view.updateStatusMessage("New colour chosen, " + colour + ", " + nextPlayer +
                            " keeps drawing cards until a " + colour + " card is chosen.");
                }

                // Regular card played
                else {
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = false;
                    view.updateStatusMessage(model.getCurrPlayer().getName() + " played a card");
                }

                // Check win condition
                if(model.isDeckEmpty()) {
                    Player winner = model.getCurrPlayer();
                    int score = model.getScore(winner);

                    //If overall winner
                    if(model.checkWinner(winner)) {
                        view.updateWinner(winner.getName(), score);
                        view.updateStatusMessage(winner.getName() + " is the Winner of the Game");
                        frame.disableAllButtons();
                    }

                    // If round winner
                    else {
                        view.updateStatusMessage(winner.getName() +  " is the Winner of the Round, with " + score + " points.");
                        String option = frame.newRoundSelectionDialog();
                        if(option != null && option.equals("New Round")) {
                            model.newRound();
                            view.updateHandPanel(model, this);
                            frame.enableCards();
                            view.updateWinner(winner.getName(), score);
                            return;

                        }
                        if(option != null && option.equals("Quit")) {
                            System.exit(0);
                            view.updateWinner(winner.getName(), score);
                            return;
                        }


                    }
                }
            }
            // Invalid move feedback
            if(cardPicked != null && !model.isPlayable(cardPicked)){
                view.updateStatusMessage("Placing that card is not a valid move. Try again.");
            }
        }
    }

    private boolean isCurrentPlayerAI() {
        if (aiPlayers == null) {
            return false;
        }
        String currentName = model.getCurrPlayer().getName();
        java.util.List<String> names = frame.getPlayerName();
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(currentName)) {
                return aiPlayers[i];
            }
        }
        return false;
    }

    private void triggerAITurnIfNeeded() {
        if (aiPlayers == null) {
            return;
        }
        if (!isCurrentPlayerAI()) {
            return;
        }

        aiTurnInProgress = true;
        try {
            performAITurns();
        } finally {
            aiTurnInProgress = false;
        }
    }

    private void performAITurns() {
        while (isCurrentPlayerAI() && !model.isDeckEmpty()) {
            Card chosen = null;
            for (Card c : model.getCurrPlayer().getPersonalDeck()) {
                if (model.isPlayable(c)) {
                    chosen = c;
                    break;
                }
            }

            if (chosen != null) {
                String cmd;
                if (chosen.getValue().equals(UnoModel.Values.WILD) ||
                        chosen.getValue().equals(UnoModel.Values.WILD_DRAW_TWO)) {
                    cmd = chosen.getValue() + "_" + System.identityHashCode(chosen);
                } else {
                    cmd = chosen.getColour() + "_" + chosen.getValue();
                }
                ActionEvent cardEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, cmd);
                actionPerformed(cardEvent);
            } else {
                ActionEvent drawEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Draw Card");
                actionPerformed(drawEvent);
            }

            if (model.isDeckEmpty()) {
                break;
            }

            ActionEvent nextEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Next Player");
            actionPerformed(nextEvent);
        }
    }

}
