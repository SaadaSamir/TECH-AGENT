package techagentprojet;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private double openingPrice;
    private double reservePrice;
    private double currentPrice;
    private List<Double> bids;
    private Map<AID, Double> buyerBids; // Map to store buyer AID and their bid amount
    private AID highestBidderAID; // Store the AID of the highest bidder from the last round
    private AID previousHighestBidderAID; // Store the AID of the highest bidder from the previous round
    private double finalPrice;
    private int numBuyers;
    private int numBidsReceived;
    private int maxBidsPerRound;
    private int currentRound;
    protected void setup() {
        openingPrice = 500;
        reservePrice = 400;
        currentPrice = openingPrice;
        bids = new ArrayList<>();
        buyerBids = new HashMap<>();
        highestBidderAID = null;
        previousHighestBidderAID = null;
        finalPrice = 0;
        numBidsReceived = 0;
        currentRound = 1;
        maxBidsPerRound = MultiAgentAuction.getNumBuyers();
        // Retrieve the number of buyers from MultiAgentAuction class
        numBuyers = MultiAgentAuction.getNumBuyers();
        System.out.println("Seller agent " + getAID().getLocalName() + " started with " + numBuyers + " buyers.");
        System.out.println("......The bidding for this extraordinary piece begins at " + (int) openingPrice + " DA. DO i get Higher !......");
        addBehaviour(new StartAuctionBehaviour());
    }

    private class StartAuctionBehaviour extends CyclicBehaviour {
        private static final long serialVersionUID = 1L;
        private long timeout = 5000; // Timeout in milliseconds (e.g., 10 seconds)

        public void action() {
            // Start new bidding round
            if(currentRound != 1) {System.out.println("..............New bid received ( " + (int) currentPrice + " DA ) Do I hear another ?..............");} 
            numBidsReceived = 0;
            bids.clear();
            buyerBids.clear();

            // Inform buyers about the new price
            informBuyersNewPrice();

            // Record the start time of the round
            long roundStartTime = System.currentTimeMillis();

            // Wait for bids from buyers with a timeout
            while (numBidsReceived < maxBidsPerRound && (System.currentTimeMillis() - roundStartTime) < timeout) {
                ACLMessage msg = receive();
                if (msg != null) {
                    double bid = Double.parseDouble(msg.getContent());
                    AID buyerAID = msg.getSender();
                    //System.out.println("Received bid from " + buyerName + ": " + (int) bid + " DA");
                    bids.add(bid);
                    buyerBids.put(buyerAID, bid);
                    numBidsReceived++;
                } else {
                    block(1000); // Check for messages every second
                }
            }

            // Update current price
            updateCurrentPrice();

            // Proceed to next round or end auction
            if (!terminationConditionMet()) {
                currentRound++;
                informBuyersNewPrice(); // Inform buyers about the new round
            } else {
                endAuction();
                informBuyersAuctionEnded(); // Inform buyers that the auction has ended
            }
        }
    }

    private void updateCurrentPrice() {
        // Find the highest bid
        double highestBid = currentPrice;
        highestBidderAID = null; // Reset highest bidder for the current round

        for (Map.Entry<AID, Double> entry : buyerBids.entrySet()) {
            if (entry.getValue() > highestBid) {
                highestBid = entry.getValue();
                highestBidderAID = entry.getKey(); // Update the highest bidder AID
            }
        }

        // Update current price
        currentPrice = highestBid;

        // Inform buyers about the current price
        //System.out.println("*******Highest bid for round " + currentRound + ": " + (int) currentPrice + " DA *******");
    }

    private void informBuyersNewPrice() {
        ACLMessage newPriceMsg = new ACLMessage(ACLMessage.INFORM);
        newPriceMsg.setContent("Current price is : " + currentPrice);
        
        // Add all active buyers to receive the new price information
        for (AID buyer : buyerBids.keySet()) {
            if (!buyer.equals(highestBidderAID)) {
                newPriceMsg.addReceiver(buyer);
            }
        }

        // Add the previous highest bidder if it's not null and not already in the list
        if (previousHighestBidderAID != null && !previousHighestBidderAID.equals(highestBidderAID)) {
            newPriceMsg.addReceiver(previousHighestBidderAID);
        }
        
        previousHighestBidderAID = highestBidderAID; // Store the highest bidder for the next round
        
        send(newPriceMsg);
    }
    
    private boolean terminationConditionMet() {
        // Define maximum number of rounds
        int maxRounds = 8; // Adjust as needed

        // Check if current round exceeds maximum rounds
        if (currentRound >= maxRounds) {
            return true; // Terminate auction if maximum rounds reached
        }

        // Check if no bids received for the current round
        if (numBidsReceived == 0) {
            // If no bids received, the previous highest bidder wins
            highestBidderAID = previousHighestBidderAID;
            return true; // Terminate auction if no bids received
        }

        return false; // Continue auction
    }

    private void endAuction() {
        // Determine winner if reserve price met
        if (currentPrice >= reservePrice) {
            finalPrice = currentPrice;
            if (highestBidderAID != null) {
                System.out.println("Item sold to highest bidder **" + highestBidderAID.getLocalName() + "** for: " + (int) finalPrice  + " DA");
            } else {
                System.out.println("Auction ended with no highest bidder.");
            }
        } else {
            System.out.println("Auction ended without meeting reserve price.");
        }

        // Terminate the seller agent after auction ends
        doDelete();
    }

    private void informBuyersAuctionEnded() {
        ACLMessage endMsg = new ACLMessage(ACLMessage.INFORM);
        if (highestBidderAID != null) {
            endMsg.setContent("Auction ended: Winner is " + highestBidderAID.getLocalName());
        } else {
            endMsg.setContent("Auction ended: No winner");
        }
        for (AID buyer : buyerBids.keySet()) {
            endMsg.addReceiver(buyer);
        }
        send(endMsg);
    }
}