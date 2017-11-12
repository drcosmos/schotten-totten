package com.boardgames.bastien.schotten_totten;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgames.bastien.schotten_totten.controllers.AbstractGameManager;
import com.boardgames.bastien.schotten_totten.exceptions.EmptyDeckException;
import com.boardgames.bastien.schotten_totten.exceptions.HandFullException;
import com.boardgames.bastien.schotten_totten.exceptions.MilestoneSideMaxReachedException;
import com.boardgames.bastien.schotten_totten.exceptions.NoPlayerException;
import com.boardgames.bastien.schotten_totten.exceptions.NotYourTurnException;
import com.boardgames.bastien.schotten_totten.model.Card;
import com.boardgames.bastien.schotten_totten.model.Hand;
import com.boardgames.bastien.schotten_totten.model.Milestone;
import com.boardgames.bastien.schotten_totten.model.MilestonePlayerType;
import com.boardgames.bastien.schotten_totten.model.Player;
import com.boardgames.bastien.schotten_totten.view.MilestoneView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class GameActivity extends AppCompatActivity {

    protected AbstractGameManager gameManager;
    protected int selectedCard;
    private boolean isClickEnabled = true;

    protected ImageButton passButton;
    protected View handLayout;
    protected TextView textView;
    protected View gameLayout;

    private final List<MilestoneView> milestoneView = new ArrayList<>();
    private final List<ImageButton> handView = new ArrayList<>();


    protected void disableClick() {
        isClickEnabled = false;
    }

    protected void enableClick() {
        isClickEnabled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_seat_game);
        passButton = (ImageButton) findViewById(R.id.passButton);
        handLayout = findViewById(R.id.handLayout);
        textView = ((TextView) findViewById(R.id.textView));
        gameLayout = findViewById(R.id.gameLayout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rules, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_settings:
//                // User chose the "Settings" item, show the app settings UI...
//                return true;

            case R.id.action_favorite:
                startActivity(new Intent(getApplicationContext(), MemoActivity.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.quit_title));

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    protected void initUI(final Hand handToUpdate) {

        for (int i = 0; i < gameManager.getMilestones().size(); i++) {
            final int id = getResources().getIdentifier("m" + i + "Milestone", "id", getPackageName());
            final ImageButton m = (ImageButton)findViewById(id);
            final int milestonePlayerSideId = getResources().getIdentifier("m" + i + "CapturedMilestonePlayerSide", "id", getPackageName());
            final int milestoneOpponentId = getResources().getIdentifier("m" + i + "CapturedMilestoneOpponentSide", "id", getPackageName());
            final ImageView mPlayer = (ImageView) findViewById(milestonePlayerSideId);
            final ImageView mOpponent = (ImageView) findViewById(milestoneOpponentId);
            final List<ImageView> pSide = new ArrayList<>();
            for (int j = 0; j < gameManager.getMilestones().get(i).MAX_CARDS_PER_SIDE; j++) {
                final int pSideId = getResources().getIdentifier("m" + i + "Card" + j + "PlayerSide", "id", getPackageName());
                pSide.add((ImageView)findViewById(pSideId));
            }
            final List<ImageView> oSide = new ArrayList<>();
            for (int j = 0; j < gameManager.getMilestones().get(i).MAX_CARDS_PER_SIDE; j++) {
                final int oSideId = getResources().getIdentifier("m" + i + "Card" + j + "OpponentSide", "id", getPackageName());
                oSide.add((ImageView)findViewById(oSideId));
            }
            milestoneView.add(new MilestoneView(m, mPlayer, mOpponent, pSide, oSide));
        }

        for (int i = 0; i < handToUpdate.getHandSize(); i++) {
            final int id = getResources().getIdentifier("h" + i, "id", getPackageName());
            handView.add((ImageButton)findViewById(id));
        }

        try {
            selectedCard = -1;

            updateTextField();

            initPassButton();

            initBoard();

            initHand(handToUpdate);

        } catch (final NoPlayerException e) {
            showErrorMessage(e);
        }
    }

    protected void initPassButton() {
        passButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    endOfTurn();
                } catch (final NoPlayerException e) {
                    showErrorMessage(e);
                }
            }
        });
        passButton.setVisibility(View.INVISIBLE);
    }

    protected void initBoard() {
        for (int i = 0; i < gameManager.getMilestones().size(); i++) {
            updateMilestoneView(i);
            milestoneView.get(i).getMilestone().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isClickEnabled) {
                        // animate
                        v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomout));

                        final ImageButton cardView = ((ImageButton) v);
                        final int index = Integer.valueOf(
                                getResources().getResourceEntryName(cardView.getId()).substring(1, 2));
                        final Milestone m = gameManager.getMilestones().get(index);
                        // check if the milestone has already been captured
                        if (!m.getCaptured().equals(MilestonePlayerType.NONE)) {
                            showAlertMessage(getString(R.string.milestone_already_captured_message));
                            return;
                        }
                        // reclaim
                        if (selectedCard == -1) {
                            // reclaim

                            // test reclaim
                            try {
                                final boolean reclaim =
                                        gameManager.reclaimMilestone(gameManager.getPlayingPlayer().getPlayerType(), index);
                                if (reclaim) {
                                    // capture the milestone
                                    updateMilestoneView(index);

                                    // check victory
                                    try {
                                        endOfTheGame(gameManager.getWinner());
                                    } catch (final NoPlayerException e) {
                                        // nothing to do, just continue to play
                                    }
                                } else {
                                    showAlertMessage(getString(R.string.cannot_capture_milestone_message));
                                }
                            } catch (final NotYourTurnException e) {
                                showErrorMessage(e);
                            }


                            // play a card
                        } else {
                            try {
                                m.checkSideSize(gameManager.getPlayingPlayer().getPlayerType());
                                // put card
                                try {
                                    gameManager.playerPlays(gameManager.getPlayingPlayer().getPlayerType(), selectedCard, index);

                                    updateMilestoneView(m.getId());

                                } catch (NotYourTurnException e) {
                                    showErrorMessage(e);
                                } catch (EmptyDeckException e) {
                                    //showAlertMessage(e.getMessage());
                                    selectedCard = -1;
                                } catch (HandFullException e) {
                                    showErrorMessage(e);
                                }
                                // update hand card;
                                try {
                                    handView.get(0).startAnimation(
                                            AnimationUtils.loadAnimation(
                                                    getApplicationContext(), R.anim.zoomout));
                                    selectCard(handView.get(0));
                                    updateHand();
                                    selectedCard = -1;
                                } catch (final NoPlayerException e) {
                                    showErrorMessage(e);
                                }

                                // end of the turn
                                disableClick();
                                passButton.setVisibility(View.VISIBLE);
                                passButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomin));

                            } catch (final MilestoneSideMaxReachedException e) {
                                // return, cannot play here
                                showAlertMessage(e.getMessage());
                            }
                        }
                    }
                }
            });
        }
    }

    protected void endOfTheGame(final Player winner) throws NoPlayerException {
        showAlertMessage(getString(R.string.end_of_the_game_title), winner.getName() + getString(R.string.end_of_the_game_message), false, false);
        passButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        textView.setText(getString(R.string.end_of_the_game_title));
        disableClick();
        passButton.setVisibility(View.VISIBLE);
        passButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomin));
    }

    protected void initHand(final Hand handToUpdate) throws NoPlayerException {
        for (int i = 0; i < handToUpdate.getHandSize(); i++) {
            final ImageButton handCardView = handView.get(i);
            updateHandCard(handCardView, handToUpdate.getCards().get(i));
            handCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isClickEnabled) {
                        // animate
                        v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomout));

                        final ImageButton cardView = ((ImageButton) v);
                        final int index = Integer.valueOf(
                                getResources().getResourceEntryName(cardView.getId()).substring(1, 2));

                        // unselect
                        for (int i = 0; i < handToUpdate.getHandSize(); i++) {
                            unSelectCard(handView.get(i));
                        }

                        // select clicked card
                        if (index == selectedCard) {
                            selectedCard = -1;
                        } else {
                            selectCard(cardView);
                            selectedCard = index;
                        }
                    }
                }
            });
            unSelectCard(handCardView);
        }
    }

    protected void selectCard(ImageButton cardView) {
        for (int i = 0; i < gameManager.getPlayingPlayer().getHand().getHandSize(); i++) {
            handView.get(i).setAlpha((float) 0.42);
        }
        cardView.setAlpha((float) 1.0);
    }

    protected void unSelectCard(ImageButton cardView) {
        for (int i = 0; i < gameManager.getPlayingPlayer().getHand().getHandSize(); i++) {
            handView.get(i).setAlpha((float) 1.0);
        }
        cardView.setAlpha((float) 1.0);
    }

    protected void updateMilestoneView(final int i) {

        // get milestone
        final Milestone milestone = gameManager.getMilestones().get(i);
        final ImageButton milestoneImageButton = milestoneView.get(i).getMilestone();
        final ImageView milestonePlayerSide = milestoneView.get(i).getMilestonePlayer();
        final ImageView milestoneOpponentSide = milestoneView.get(i).getMilestoneOpponent();
        // update milestones views
        if (milestone.getCaptured().equals(gameManager.getPlayingPlayer().getPlayerType())) {
            milestoneImageButton.setVisibility(View.INVISIBLE);
            milestonePlayerSide.setVisibility(View.VISIBLE);
            milestoneOpponentSide.setVisibility(View.INVISIBLE);
        } else if (milestone.getCaptured().equals(MilestonePlayerType.NONE)) {
            milestoneImageButton.setVisibility(View.VISIBLE);
            milestonePlayerSide.setVisibility(View.INVISIBLE);
            milestoneOpponentSide.setVisibility(View.INVISIBLE);
        } else {
            milestoneImageButton.setVisibility(View.INVISIBLE);
            milestonePlayerSide.setVisibility(View.INVISIBLE);
            milestoneOpponentSide.setVisibility(View.VISIBLE);
        }

        final List<ImageView> playerSide = milestoneView.get(i).getPlayerSide();
        final List<ImageView> opponentSide = milestoneView.get(i).getOpponentSide();

        // reset all cards on both sides
        for (int j = 0; j < milestone.MAX_CARDS_PER_SIDE; j++) {
            resetPlayedCard(playerSide.get(j));
            resetPlayedCard(opponentSide.get(j));
        }
        // update player 1 side
        for (int iP1 = 0; iP1 < milestone.getPlayer1Side().size(); iP1++) {
            final List<ImageView> side = (gameManager.getPlayingPlayer().getPlayerType().equals(MilestonePlayerType.ONE)) ?
                    playerSide: opponentSide;
            final Card card = milestone.getPlayer1Side().get(iP1);
            updatePlayedCard(side.get(iP1), card);
            if (!gameManager.getPlayingPlayer().getPlayerType().equals(MilestonePlayerType.ONE)
                    && card.getColor().equals(gameManager.getLastPlayedCard().getColor())
                    && card.getNumber().equals(gameManager.getLastPlayedCard().getNumber())) {
                side.get(iP1).startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomout));
            }
        }
        // update player 2 side
        for (int iP2 = 0; iP2 < milestone.getPlayer2Side().size(); iP2++) {
            final List<ImageView> side = (gameManager.getPlayingPlayer().getPlayerType().equals(MilestonePlayerType.TWO)) ?
                    playerSide: opponentSide;
            final Card card =milestone.getPlayer2Side().get(iP2);
            updatePlayedCard(side.get(iP2), card);
            if (!gameManager.getPlayingPlayer().getPlayerType().equals(MilestonePlayerType.TWO)
                    && card.getColor().equals(gameManager.getLastPlayedCard().getColor())
                    && card.getNumber().equals(gameManager.getLastPlayedCard().getNumber())) {
                side.get(iP2).startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomout));
            }
        }

    }

    private void updatePlayedCard(final ImageView view, final Card card) {
        updateCard(view, card);
    }

    private void updateHandCard(final ImageButton view, final Card card) {
        updateCard(view, card);
    }

    private void updateCard(final ImageView view, final Card card) {
        switch (card.getNumber()) {
            case ONE:
                view.setImageResource(R.drawable.number1one);
                break;
            case TWO:
                view.setImageResource(R.drawable.number2two);
                break;
            case THREE:
                view.setImageResource(R.drawable.number3three);
                break;
            case FOUR:
                view.setImageResource(R.drawable.number4four);
                break;
            case FIVE:
                view.setImageResource(R.drawable.number5five);
                break;
            case SIX:
                view.setImageResource(R.drawable.number6six);
                break;
            case SEVEN:
                view.setImageResource(R.drawable.number7seven);
                break;
            case EIGHT:
                view.setImageResource(R.drawable.number8eight);
                break;
            case NINE:
                view.setImageResource(R.drawable.number9nine);
                break;


        }
        switch (card.getColor()) {
            case BLUE:
                view.setBackgroundColor(Color.BLUE);
                break;
            case YELLOW:
                view.setBackgroundColor(Color.YELLOW);
                break;
            case GREEN:
                view.setBackgroundColor(Color.GREEN);
                break;
            case GREY:
                view.setBackgroundColor(Color.GRAY);
                break;
            case RED:
                view.setBackgroundColor(Color.RED);
                break;
            case CYAN:
                view.setBackgroundColor(Color.CYAN);
                break;
        }
    }

    private void resetPlayedCard(final ImageView view) {
        view.setImageResource(R.drawable.empty);
        view.setBackgroundColor(Color.LTGRAY);
    }

    protected abstract void endOfTurn() throws NoPlayerException;

    protected void updateTextField() throws NoPlayerException {
        textView.setText(gameManager.getPlayingPlayer().getName() + getString(R.string.it_is_your_turn_message));
    }

    private void updateHand() throws NoPlayerException {
        // update hand
        final Hand hand = gameManager.getPlayingPlayer().getHand();
        for (int i = 0; i < hand.getHandSize(); i++) {
            final ImageButton handCardView = handView.get(i);
            updateHandCard(handCardView, hand.getCards().get(i));
            unSelectCard(handCardView);
            handCardView.setVisibility(View.VISIBLE);
        }
        // cards if hand is not full (no more cards to draw)
        for (int i = hand.getHandSize(); i < hand.MAX_HAND_SIZE; i++) {
            final ImageButton handCardView = handView.get(i);
            unSelectCard(handCardView);
            handCardView.setVisibility(View.INVISIBLE);
        }
    }

    protected void updateUI() throws NoPlayerException {
        // update board
        for (int i = 0; i < gameManager.getMilestones().size(); i++) {
            updateMilestoneView(i);
        }

        handLayout.setVisibility(View.VISIBLE);
        updateHand();

        // update playing player text
        updateTextField();

        // show/hide skip button
        passButton.setVisibility(View.VISIBLE);
        for (final Milestone m : gameManager.getMilestones()) {
            if (m.getCaptured().equals(MilestonePlayerType.NONE)) {
                try {
                    m.checkSideSize(gameManager.getPlayingPlayer().getPlayerType());
                    passButton.setVisibility(View.INVISIBLE);
                    break;
                } catch (final MilestoneSideMaxReachedException e) {
                    // nothing to do, just test next milestone
                }
            }
        }
    }



    protected void showErrorMessage(final Exception e) {
        final StringWriter message = new StringWriter();
        e.printStackTrace(new PrintWriter(message));
        showAlertMessage(getString(R.string.error_title) + e.getMessage(), message.toString(), true, true);
    }

    protected void showAlertMessage(final String message) {
        showAlertMessage(getString(R.string.warning_title), message, false, false);
    }
    protected void showAlertMessage(final String title, final String message, final boolean finish, final boolean hideBoard) {
        if (hideBoard) {
            gameLayout.setVisibility(View.INVISIBLE);
        }
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        gameLayout.setVisibility(View.VISIBLE);
                        dialog.dismiss();
                        if (finish) {
                            finish();
                        }
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.show();

    }
}
