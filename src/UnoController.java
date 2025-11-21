import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    }

    /**
     * Starts the game by:
     * - Adding players from the frame
     * - Initializing a new round
     * - Updating the view and hand panel
     * - Enabling card interaction
     */
    public void play() {
        for(String player: frame.getPlayerName()) {
            model.addPlayer(player);
        }
        model.newRound();
        //view.update(model);
        view.updateHandPanel(model, this);
        frame.enableCards();
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
            //view.update(model);
            view.updateHandPanel(model, this);
            frame.enableCards();

        }

        // Handle Draw Card button
        if(e.getActionCommand().equals("Draw Card")) {
            frame.getNextButton().setEnabled(true);
            model.drawCard();                              // Draw card into player's hand
            isAdvanced = false;
            view.updateHandPanel(model, this);
            frame.disableCards();                          // Disable cards until next turn
            view.updateStatusMessage(model.getCurrPlayer().getName() + " draws a card.");

        }

        // Handle card selections
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
                model.setTopCard(cardPicked);               // Update the top card

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

                else if(cardPicked.getValueDark().equals(UnoModel.ValuesDark.DRAW_FIVE)) {
                    model.drawFive();
                    view.updateHandPanel(model, this);
                    frame.disableCards();
                    isAdvanced = true;
                    String nextPlayer = model.getNextPlayer().getName();
                    view.updateStatusMessage("Draw five played and " + nextPlayer + " picks up 5 cards and skips their turn.");
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
}