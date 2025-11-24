/**
 * JUnit Tests for UnoModel (Milestone 2)
 * 
 * This test suite verifies:
 *  - Random card generation
 *  - Dealing cards on new round
 *  - Playing and drawing cards
 *  - Special card actions (reverse, skip, wild, wild draw two)
 *  - Turn order logic (advance + nextPlayer)
 *  - Scoring and deck-empty checks
 *  - Basic model functionality such as top card setting and card playability
 */

import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class UnoModelTest {

  private UnoModel model;

  /** Sets up a basic game with two players before every test. */
  @Before
  public void setUp() {
    model = new UnoModel();
    model.addPlayer("John");
    model.addPlayer("Mark");
  }

  /** Ensures getRandomCard() always returns a valid card. */
  @Test
  public void testGetRandomCard() {
    for (int i = 0; i < 20; i++) {
      UnoModel.Card card = model.getRandomCard();
      assertNotNull(card);
      assertNotNull(card.getValue());

      // Non-wild cards must have a colour
      if (card.getValue() != UnoModel.Values.WILD &&
          card.getValue() != UnoModel.Values.WILD_DRAW_TWO) {
        assertNotNull(card.getColour());
      }
    }
  }

  /** Playing a card removes it from hand and updates top card. */
  @Test
  public void testPlayCardTop() {
    model.newRound();
    UnoModel.Player player = model.getCurrPlayer();
    UnoModel.Card first = player.getPersonalDeck().get(0);

    int before = player.getPersonalDeck().size();

    model.playCard(first);

    assertEquals(before - 1, player.getPersonalDeck().size());
    assertEquals(first, model.getTopCard());
  }

  /** newRound(): every player must receive exactly 7 cards and top card cannot be wild. */
  @Test
  public void testNewRoundSevenCards() {
    model.addPlayer("Lina");
    model.newRound();

    assertEquals(7, model.getCurrPlayer().getPersonalDeck().size());
    model.advance();
    assertEquals(7, model.getCurrPlayer().getPersonalDeck().size());
    model.advance();
    assertEquals(7, model.getCurrPlayer().getPersonalDeck().size());

    UnoModel.Card top = model.getTopCard();
    assertNotNull(top);
    assertTrue(top.getValue() != UnoModel.Values.WILD &&
               top.getValue() != UnoModel.Values.WILD_DRAW_TWO);
  }

  /** drawCard() must add exactly one new card to the current player's hand. */
  @Test
  public void testDrawCardAddsOne() {
    model.newRound();
    int before = model.getCurrPlayer().getPersonalDeck().size();
    model.drawCard();
    assertEquals(before + 1, model.getCurrPlayer().getPersonalDeck().size());
  }

  /** drawOne(): next player should get 1 extra card. */
  @Test
  public void testDrawOneNextPlayer() {
    model = new UnoModel();
    model.addPlayer("John");
    model.addPlayer("Mark");
    model.newRound();

    UnoModel.Card c = model.drawOne();
    assertNotNull(c);

    model.advance();
    assertEquals(8, model.getCurrPlayer().getPersonalDeck().size());
  }

  /** reverse() + skip() must produce expected turn order. */
  @Test
  public void testSkipAfterReverse() {
    model = new UnoModel();
    model.addPlayer("A");
    model.addPlayer("B");
    model.addPlayer("C");
    model.newRound();

    assertEquals("A", model.getCurrPlayer().getName());

    // Normal skip
    UnoModel normalGame = new UnoModel();
    normalGame.addPlayer("A");
    normalGame.addPlayer("B");
    normalGame.addPlayer("C");
    normalGame.newRound();
    normalGame.skip();
    assertEquals("C", normalGame.getCurrPlayer().getName());

    // Reverse + skip
    model.reverse();
    model.skip();
    assertEquals("B", model.getCurrPlayer().getName());
  }

  /** getNextPlayer() must change with direction + current index. */
  @Test
  public void testNextPlayer() {
    model = new UnoModel();
    model.addPlayer("A");
    model.addPlayer("B");
    model.addPlayer("C");
    model.newRound();

    assertEquals("B", model.getNextPlayer().getName());
    model.reverse();
    assertEquals("C", model.getNextPlayer().getName());
    model.advance();
    assertEquals("B", model.getNextPlayer().getName());
  }

  /** wild(colour) must set the top card's colour. */
  @Test
  public void testWildSetsColour() {
    model.newRound();
    model.wild(UnoModel.Colours.RED);
    assertEquals(UnoModel.Colours.RED, model.getTopCard().getColour());
  }

  /** wildDrawTwo(): sets a new colour, gives next player 2 cards, then skips them. */
  @Test
  public void testWildDrawTwoAddsCards() {
    model = new UnoModel();
    model.addPlayer("John");
    model.addPlayer("Mark");
    model.newRound();

    model.advance();
    int markStart = model.getCurrPlayer().getPersonalDeck().size();

    // Reset and test the actual function
    model = new UnoModel();
    model.addPlayer("John");
    model.addPlayer("Mark");
    model.newRound();

    List<UnoModel.Card> drawn = model.wildDrawTwo(UnoModel.Colours.GREEN);

    assertNotNull(drawn);
    assertEquals(2, drawn.size());
    assertEquals(UnoModel.Colours.GREEN, model.getTopCard().getColour());

    model.advance();
    assertEquals(markStart + 2, model.getCurrPlayer().getPersonalDeck().size());
  }

  /**
   * isPlayable() must allow:
   *  - same colour
   *  - same value
   *  - wild cards
   * And reject cards with different colour + value.
   */
  @Test
  public void testPlayableCard() {
    model.newRound();
    UnoModel.Card top = model.getTopCard();

    // Same colour
    UnoModel.Card sameColour = new UnoModel.Card(top.getColour(), UnoModel.Values.FIVE);
    assertTrue(model.isPlayable(sameColour));

    // Same value
    UnoModel.Colours otherColour =
        (top.getColour() == UnoModel.Colours.RED) ? UnoModel.Colours.BLUE : UnoModel.Colours.RED;

    UnoModel.Card sameValue = new UnoModel.Card(otherColour, top.getValue());
    assertTrue(model.isPlayable(sameValue));

    // Wild
    UnoModel.Card wild = new UnoModel.Card(null, UnoModel.Values.WILD);
    assertTrue(model.isPlayable(wild));

    // Neither colour nor value matches
    UnoModel.Values diffValue =
        (top.getValue() == UnoModel.Values.ONE) ? UnoModel.Values.TWO : UnoModel.Values.ONE;

    UnoModel.Card neither = new UnoModel.Card(otherColour, diffValue);

    boolean differsColour = (top.getColour() == null) || (otherColour != top.getColour());
    boolean differsValue  = diffValue != top.getValue();

    if (differsColour && differsValue) {
        assertFalse(model.isPlayable(neither));
    }
  }

  /** advance() must move to a new player. */
  @Test
  public void testAdvanceNextPlayer() {
    model.addPlayer("Max");
    model.newRound();

    String first = model.getCurrPlayer().getName();
    model.advance();
    String second = model.getCurrPlayer().getName();

    assertNotEquals(first, second);
  }

  /** Score calculation example test (expected score = 71). */
  @Test
  public void testScoreTotal() {
    model.newRound();
    UnoModel.Player winner = model.getCurrPlayer();

    model.advance();
    UnoModel.Player opponent = model.getCurrPlayer();

    opponent.getPersonalDeck().clear();
    opponent.getPersonalDeck().add(new UnoModel.Card(UnoModel.Colours.RED, UnoModel.Values.ONE));       // 1 point
    opponent.getPersonalDeck().add(new UnoModel.Card(UnoModel.Colours.YELLOW, UnoModel.Values.SKIP));   // 20 points
    opponent.getPersonalDeck().add(new UnoModel.Card(null, UnoModel.Values.WILD));                      // 50 points

    int score = model.getScore(winner);

    assertEquals(71, score);
  }

  /** isDeckEmpty() must reflect whether current player's hand is empty or not. */
  @Test
  public void testIsDeckEmpty() {
    model.newRound();

    assertFalse(model.isDeckEmpty());

    model.getCurrPlayer().getPersonalDeck().clear();
    assertTrue(model.isDeckEmpty());

    model.drawCard();
    assertFalse(model.isDeckEmpty());
  }

  /** setTopCard() must overwrite the previous top card correctly. */
  @Test
  public void testSetTopCard() {
    model.newRound();

    UnoModel.Card chosen =
        new UnoModel.Card(UnoModel.Colours.BLUE, UnoModel.Values.THREE);

    model.setTopCard(chosen);

    assertEquals(chosen, model.getTopCard());
  }
}
