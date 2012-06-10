/**
 *  Copyright (C) 2002-2012   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTradeItem;


/**
 * The panel that allows negotiations between players.
 */
public final class NegotiationDialog extends FreeColDialog<DiplomaticTrade> implements ActionListener {

    private static Logger logger = Logger.getLogger(NegotiationDialog.class.getName());

    private static final String SEND = "send", ACCEPT = "accept", CANCEL = "cancel";

    private static final int HUGE_DEMAND = 100000;

    private DiplomaticTrade agreement;

    private JButton acceptButton, cancelButton, sendButton;
    private StanceTradeItemPanel stance;
    private GoldTradeItemPanel goldOffer, goldDemand;
    private ColonyTradeItemPanel colonyOffer, colonyDemand;
    private GoodsTradeItemPanel goodsOffer, goodsDemand;
    //private UnitTradeItemPanel unitOffer, unitDemand;
    private JPanel summary;

    private final Unit unit;
    private final Settlement settlement;
    private Player player;
    private Player otherPlayer;
    private Player sender;
    private Player recipient;
    private boolean canAccept;

    private String demandMessage;
    private String offerMessage;
    private String exchangeMessage;



    /**
     * Creates a new <code>NegotiationDialog</code> instance.
     *
     * @param unit an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     */
    public NegotiationDialog(FreeColClient freeColClient, GUI gui, Unit unit, Settlement settlement) {
        this(freeColClient, gui, unit, settlement, null);
    }

    /**
     * Creates a new <code>NegotiationDialog</code> instance.
     * @param freeColClient
     *
     * @param unit an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     * @param agreement a <code>DiplomaticTrade</code> with the offer
     */
    public NegotiationDialog(FreeColClient freeColClient, GUI gui, Unit unit, Settlement settlement, DiplomaticTrade agreement) {
        super(freeColClient, gui);
        setFocusCycleRoot(true);

        this.unit = unit;
        this.settlement = settlement;
        this.player = getMyPlayer();
        this.sender = unit.getOwner();
        this.recipient = settlement.getOwner();
        this.canAccept = agreement != null; // a new offer can't be accepted
        if (agreement == null) {
            this.agreement = new DiplomaticTrade(unit.getGame(), sender, recipient);
        } else {
            this.agreement = agreement;
        }
        if (sender == player) {
            this.otherPlayer = recipient;
        } else {
            this.otherPlayer = sender;
        }

        demandMessage =
            Messages.message(StringTemplate.template("negotiationDialog.demand")
                             .addStringTemplate("%nation%", sender.getNationName()));
        offerMessage =
            Messages.message(StringTemplate.template("negotiationDialog.offer")
                             .addStringTemplate("%nation%", sender.getNationName()));
        exchangeMessage =
            Messages.message(StringTemplate.template("negotiationDialog.exchange")
                             .addStringTemplate("%nation%", sender.getNationName()));

        if (player.atWarWith(otherPlayer)) {
            if (!hasPeaceOffer()) {
                Stance stance = Stance.PEACE;
                this.agreement.add(new StanceTradeItem(getGame(), player, otherPlayer, stance));
            }
        }

        summary = new JPanel(new MigLayout("wrap 2", "[20px][]"));
        summary.setOpaque(false);
    }

    /**
     * Set up the dialog.
     *
     */
    @Override
    public void initialize() {

        sendButton = new JButton(Messages.message("negotiationDialog.send"));
        sendButton.addActionListener(this);
        sendButton.setActionCommand(SEND);
        FreeColPanel.enterPressesWhenFocused(sendButton);

        acceptButton = new JButton(Messages.message("negotiationDialog.accept"));
        acceptButton.addActionListener(this);
        acceptButton.setActionCommand(ACCEPT);
        FreeColPanel.enterPressesWhenFocused(acceptButton);
        acceptButton.setEnabled(canAccept);

        cancelButton = new JButton(Messages.message("negotiationDialog.cancel"));
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(CANCEL);
        setCancelComponent(cancelButton);
        FreeColPanel.enterPressesWhenFocused(cancelButton);

        stance = new StanceTradeItemPanel(this, player, otherPlayer);
        goldDemand = new GoldTradeItemPanel(this, otherPlayer, HUGE_DEMAND);
        goldOffer = new GoldTradeItemPanel(this, player,
            ((player.getGold() == Player.GOLD_NOT_ACCOUNTED) ? HUGE_DEMAND
                : player.getGold()));
        colonyDemand = new ColonyTradeItemPanel(this, otherPlayer);
        colonyOffer = new ColonyTradeItemPanel(this, player);
        /** TODO: UnitTrade
            unitDemand = new UnitTradeItemPanel(this, otherPlayer);
            unitOffer = new UnitTradeItemPanel(this, player);
        */

        setLayout(new MigLayout("wrap 3", "[200, fill][300, fill][200, fill]", ""));

        add(new JLabel(demandMessage), "center");
        add(new JLabel(offerMessage), "skip, center");

        add(goldDemand);
        add(summary, "spany, top");
        add(goldOffer);
        if (unit.isCarrier()) {
            goodsDemand = new GoodsTradeItemPanel(this, otherPlayer, null);
            add(goodsDemand);
            goodsOffer = new GoodsTradeItemPanel(this, player, null);
            add(goodsOffer);
        } else {
            add(colonyDemand);
            add(colonyOffer);
        }
        add(stance, "skip");
        /** TODO: UnitTrade
            add(unitDemand, higConst.rc(row, demandColumn));
            add(unitOffer, higConst.rc(row, offerColumn));
        */
        add(sendButton, "newline 20, span, split 3");
        add(acceptButton, "tag ok");
        add(cancelButton, "tag cancel");

        updateDialog();
    }

    private void updateSummary() {
        summary.removeAll();

        List<TradeItem> offers = getItemsFor(sender);
        List<TradeItem> demands = getItemsFor(recipient);

        if (!offers.isEmpty()) {
            summary.add(new JLabel(offerMessage), "span");
            for (TradeItem item : offers) {
                summary.add(getTradeItemButton(item), "skip");
            }
        }
        if (!demands.isEmpty()) {
            if (offers.isEmpty()) {
                summary.add(new JLabel(demandMessage), "span");
            } else {
                summary.add(new JLabel(exchangeMessage), "newline 20, span");
            }
            for (TradeItem item : demands) {
                summary.add(getTradeItemButton(item), "skip");
            }
        }

    }

    private void updateOfferItems() {
        // Update stance options
        stance.updateStanceBox();

        // Update the gold amount that can be demanded
        goldOffer.setAvailableGold(player.getGold());

        if(unit.isCarrier()){
            Iterator<Goods> goodsInAgreement = agreement.getGoodsGivenBy(player).iterator();
            List<Goods> goodsAvail = new ArrayList<Goods>();
            goodsAvail.addAll(unit.getGoodsContainer().getGoods());

            //remove the ones already on the table
            while(goodsInAgreement.hasNext()){
                Goods goods = goodsInAgreement.next();
                for(int i=0;i<goodsAvail.size();i++){
                    Goods goodAvail = goodsAvail.get(i);
                    if(goodAvail.getType() == goods.getType() &&
                       goodAvail.getAmount() == goods.getAmount()){
                        // this good is already on the agreement, remove it
                        goodsAvail.remove(i);
                        break;
                    }
                }
            }

            // Update the list of goods available to add to agreement
            goodsOffer.updateGoodsBox(goodsAvail);
        } else{
            // Update the list of colonies available to add to agreement
            colonyOffer.updateColonyBox();
        }
    }

    private void updateDemandItems() {
        // Update the gold amount that can be demanded
        NationSummary ns = getController().getNationSummary(otherPlayer);
        int foreignGold = (ns == null) ? 0 : ns.getGold();
        goldDemand.setAvailableGold(foreignGold);

        if(unit.isCarrier()){
            Iterator<Goods> goodsInAgreement = agreement.getGoodsGivenBy(otherPlayer).iterator();
            List<Goods> goodsAvail = new ArrayList<Goods>();
            goodsAvail.addAll(settlement.getGoodsContainer().getGoods());

            //remove the ones already on the table
            while(goodsInAgreement.hasNext()){
                Goods goods = goodsInAgreement.next();
                for(int i=0;i<goodsAvail.size();i++){
                    Goods goodAvail = goodsAvail.get(i);
                    if(goodAvail.getType() == goods.getType() &&
                       goodAvail.getAmount() == goods.getAmount()){
                        // this good is already on the agreement, remove it
                        goodsAvail.remove(i);
                        break;
                    }
                }
            }

            // Update the list of goods available to add to agreement
            goodsDemand.updateGoodsBox(goodsAvail);
        } else {
            // Update the list of colonies available to add to agreement
            colonyDemand.updateColonyBox();
        }
    }

    public void updateDialog(){
        updateOfferItems();
        updateDemandItems();
        updateSummary();
    }

    private List<TradeItem> getItemsFor(Player source) {
        List<TradeItem> result = new ArrayList<TradeItem>();
        for (TradeItem item : agreement.getTradeItems()) {
            if (item.getSource() == source) {
                result.add(item);
            }
        }
        return result;
    }

    private JButton getTradeItemButton(TradeItem item) {
        String description = null;
        if (item instanceof StanceTradeItem) {
            description = Messages.getStanceAsString(((StanceTradeItem) item).getStance());
        } else if (item instanceof GoldTradeItem) {
            int gold = ((GoldTradeItem) item).getGold();
            description = Messages.message(StringTemplate.template("tradeItem.gold.long")
                                           .addAmount("%amount%", gold));
        } else if (item instanceof ColonyTradeItem) {
            description = Messages.message(StringTemplate.template("tradeItem.colony.long")
                                           .addName("%colony%", ((ColonyTradeItem) item).getColonyName()));
        } else if (item instanceof GoodsTradeItem) {
            description = Messages.message(StringTemplate.template("model.goods.goodsAmount")
                                           .addAmount("%amount%", ((GoodsTradeItem) item).getGoods().getAmount())
                                           .add("%goods%", ((GoodsTradeItem) item).getGoods().getNameKey()));
        } else if (item instanceof UnitTradeItem) {
            description = Messages.message(((UnitTradeItem) item).getUnit().getLabel());
        }
        JButton button = new JButton(new RemoveAction(item));
        button.setText(description);
        button.setMargin(emptyMargin);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private boolean hasPeaceOffer() {
        return (getStance() != null);
    }


    /**
     * Adds a <code>ColonyTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param colony a <code>Colony</code> value
     */
    public void addColonyTradeItem(Player source, Colony colony) {
        Player destination;
        if (source == otherPlayer) {
            destination = player;
        } else {
            destination = otherPlayer;
        }
        agreement.add(new ColonyTradeItem(getGame(), source, destination, colony));
    }

    /**
     * Adds a <code>GoldTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param amount an <code>int</code> value
     */
    public void addGoldTradeItem(Player source, int amount) {
        Player destination;
        if (source == otherPlayer) {
            destination = player;
        } else {
            destination = otherPlayer;
        }
        agreement.add(new GoldTradeItem(getGame(), source, destination, amount));
    }

    /**
     * Adds a <code>GoodsTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param goods a <code>Goods</code> value
     */
    public void addGoodsTradeItem(Player source, Goods goods) {
        Player destination;
        if (source == otherPlayer) {
            destination = player;
        } else {
            destination = otherPlayer;
        }
        agreement.add(new GoodsTradeItem(getGame(), source, destination, goods, settlement));
    }



    /**
     * Trade a stance change between the players.
     *
     * @param stance The <code>Stance</code> to trade.
     */
    public void setStance(Stance stance) {
        agreement.add(new StanceTradeItem(getGame(), otherPlayer, player, stance));
    }


    /**
     * Returns the stance being offered.
     *
     * @return a <code>Stance</code> value
     */
    public Stance getStance() {
        return agreement.getStance();
    }

    private class RemoveAction extends AbstractAction {
        private TradeItem item;

        public RemoveAction(TradeItem item) {
            this.item = item;
        }

        public void actionPerformed(ActionEvent e) {
            agreement.remove(item);
            updateDialog();
        }
    }


    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     *
     * @param event The incoming action event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(CANCEL)) {
            agreement.setStatus(TradeStatus.REJECT_TRADE);
            setResponse(agreement);
        } else if (command.equals(ACCEPT)) {
            agreement.setStatus(TradeStatus.ACCEPT_TRADE);
            setResponse(agreement);
        } else if (command.equals(SEND)) {
            agreement.setStatus(TradeStatus.PROPOSE_TRADE);
            setResponse(agreement);
        }
    }

    public class ColonyTradeItemPanel extends JPanel implements ActionListener {

        private JComboBox colonyBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;
        private JLabel textLabel;

        /**
         * Creates a new <code>ColonyTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         */
        public ColonyTradeItemPanel(NegotiationDialog parent, Player source) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            colonyBox = new JComboBox();
            updateColonyBox();

            setLayout(new MigLayout("wrap 1", "", ""));
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            this.textLabel = new JLabel(Messages.message("tradeItem.colony"));
            add(this.textLabel);
            add(colonyBox);
            add(addButton);

        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        private void updateColonyBox() {

            if (!player.isEuropean()) {
                return;
            }

            // Remove all action listeners, so the update has no effect (except
            // updating the list).
            ActionListener[] listeners = colonyBox.getActionListeners();
            for (ActionListener al : listeners) {
                colonyBox.removeActionListener(al);
            }
            colonyBox.removeAllItems();

            Iterator<Colony> coloniesInAgreement = agreement.getColoniesGivenBy(player).iterator();
            List<Colony> coloniesAvail = getClientOptions().getSortedColonies(player);

            //remove the ones already on the table
            while(coloniesInAgreement.hasNext()){
                Colony colony = coloniesInAgreement.next();
                for(int i=0;i<coloniesAvail.size();i++){
                    Colony colonyAvail = coloniesAvail.get(i);
                    if(colonyAvail == colony){
                        // this good is already on the agreement, remove it
                        coloniesAvail.remove(i);
                        break;
                    }
                }
            }

            if (coloniesAvail.isEmpty()){
                addButton.setEnabled(false);
                colonyBox.setEnabled(false);
            } else {
                for (Colony c : coloniesAvail) {
                    colonyBox.addItem(c);
                }
                for(ActionListener al : listeners) {
                    colonyBox.addActionListener(al);
                }
                addButton.setEnabled(true);
                colonyBox.setEnabled(true);
            }
        }

        /**
         * Analyzes an event and calls the right external methods to take care of
         * the user's request.
         *
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                negotiationDialog.addColonyTradeItem(player, (Colony) colonyBox.getSelectedItem());
                updateDialog();
            }

        }
    }

    public class GoodsTradeItemPanel extends JPanel implements ActionListener {

        private JComboBox goodsBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;
        private JLabel label;

        /**
         * Creates a new <code>GoodsTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         * @param allGoods a <code>List</code> of <code>Goods</code> values
         */
        public GoodsTradeItemPanel(NegotiationDialog parent, Player source, List<Goods> allGoods) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            goodsBox = new JComboBox();
            this.label = new JLabel(Messages.message("tradeItem.goods"));

            updateGoodsBox(allGoods);

            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new MigLayout("wrap 1", "", ""));
            add(label);
            add(goodsBox);
            add(addButton);
            setSize(getPreferredSize());
        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        private void updateGoodsBox(List<Goods> allGoods) {

            // Remove all action listeners, so the update has no effect (except
            // updating the list).
            ActionListener[] listeners = goodsBox.getActionListeners();
            for (ActionListener al : listeners) {
                goodsBox.removeActionListener(al);
            }
            goodsBox.removeAllItems();

            if(allGoods != null && !allGoods.isEmpty()){
                Iterator<Goods> goodsIterator = allGoods.iterator();
                while (goodsIterator.hasNext()) {
                    Goods goods = goodsIterator.next();
                    if(goods.getType().isStorable()){
                        goodsBox.addItem(goods);
                    }
                }
                for(ActionListener al : listeners) {
                    goodsBox.addActionListener(al);
                }

                this.label.setEnabled(true);
                addButton.setEnabled(true);
                goodsBox.setEnabled(true);
            } else{
                this.label.setEnabled(false);
                addButton.setEnabled(false);
                goodsBox.setEnabled(false);
            }
        }

        /**
         * Analyzes an event and calls the right external methods to take care of
         * the user's request.
         *
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                negotiationDialog.addGoodsTradeItem(player, (Goods) goodsBox.getSelectedItem());
                updateDialog();
            }

        }
    }

    public class StanceTradeItemPanel extends JPanel implements ActionListener {

        class StanceItem {
            private Stance value;

            StanceItem (Stance value) {
               if (value == null)
                  throw new NullPointerException();
               this.value = value;
            }

            @Override
            public String toString() {
                return Messages.getStanceAsString(value);
            }

            Stance getValue() {
                return value;
            }

            @Override
            public boolean equals(Object other) {
                if (other == null || !(other instanceof StanceItem)) {
                    return false;
                }
                return value.equals(((StanceItem) other).value);
            }

            @Override
            public int hashCode()
            {
               return value.hashCode();
            }
        }

        private JComboBox stanceBox;
        private JButton addButton;
        private NegotiationDialog negotiationDialog;
        private Player source;
        private Player target;

        /**
         * Creates a new <code>StanceTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         * @param target <code>Player</code>
         */
        public StanceTradeItemPanel(NegotiationDialog parent, Player source, Player target) {
            this.negotiationDialog = parent;
            this.source = source;
            this.target = target;

            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            stanceBox = new JComboBox();

            updateStanceBox();

            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new MigLayout("wrap 1", "", ""));
            add(new JLabel(Messages.message("tradeItem.stance")));
            add(stanceBox);
            add(addButton);
        }

        /**
         * Analyzes an event and calls the right external methods to take care of
         * the user's request.
         *
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                StanceItem stance = (StanceItem) stanceBox.getSelectedItem();
                negotiationDialog.setStance(stance.getValue());
                updateSummary();
            }

        }

        @SuppressWarnings("unchecked") // FIXME in Java7
        public void updateStanceBox(){
            stanceBox.removeAllItems();
            Stance stance = source.getStance(target);
            if (stance != Stance.WAR) stanceBox.addItem(new StanceItem(Stance.WAR));
            if (stance == Stance.WAR) stanceBox.addItem(new StanceItem(Stance.CEASE_FIRE));
            if (stance != Stance.PEACE && stance != Stance.UNCONTACTED) stanceBox.addItem(new StanceItem(Stance.PEACE));
            if (stance != Stance.ALLIANCE) stanceBox.addItem(new StanceItem(Stance.ALLIANCE));
            if (negotiationDialog.hasPeaceOffer()) {
                stanceBox.setSelectedItem(new StanceItem(negotiationDialog.getStance()));
            }
        }
    }

    public class GoldTradeItemPanel extends JPanel implements ActionListener {

        private JSpinner spinner;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

        /**
         * Creates a new <code>GoldTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         * @param gold int ??
         */
        public GoldTradeItemPanel(NegotiationDialog parent, Player source, int gold) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, gold, 1));
            // adjust entry size
            ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setColumns(5);

            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new MigLayout("wrap 1", "", ""));
            add(new JLabel(Messages.message("tradeItem.gold")));
            add(spinner);
            add(addButton);
        }

        /**
         * Analyzes an event and calls the right external methods to
         * take care of the user's request.
         *
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                int amount = ((Integer) spinner.getValue()).intValue();
                negotiationDialog.addGoldTradeItem(player, amount);
                updateDialog();
            }

        }

        public void setAvailableGold(int gold) {
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            model.setMaximum(new Integer(gold));
        }
    }

}
