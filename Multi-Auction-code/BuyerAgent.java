package techagentprojet;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.concurrent.CountDownLatch;

public class BuyerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private double budget;
    private double startingBudget; // Store the starting budget
    private static final CountDownLatch latch = MultiAgentAuction.getLatch();

    protected void setup() {
        startingBudget = Math.random() * 2000 + 500; // Random budget between 500 and 1500
        budget = startingBudget; // Initialize budget
        System.out.println("Buyer agent " + getLocalName() + " created and started with budget: " + (int) budget + " DA");
        try {
            // Wait for all agents to be created and started
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Add a one-shot behavior to initiate bidding after all agents are created
        addBehaviour(new InitiateBidding());
    }

    private class InitiateBidding extends OneShotBehaviour {
        private static final long serialVersionUID = 1L;

        public void action() {
            // Start bidding process
            addBehaviour(new BidBehaviour());
        }
    }

    private class BidBehaviour extends CyclicBehaviour {
        private static final long serialVersionUID = 1L;
        private boolean hasBid = false;
        private double currentPrice = 500; // Initial price set by the seller
        private double lastBid = 0; // Store the last bid made by the buyer
        private boolean isFirstRound = true; // Flag to indicate if it's the first round
        private int numBuyers; // Number of buyers participating in the auction
        private boolean wasAlone = false; // Flag to indicate if the buyer was alone in the previous round
        private boolean highestBidder = false; // Flag to indicate if the buyer was the highest bidder in the previous round

        public void action() {
            if (isFirstRound) {
                numBuyers = MultiAgentAuction.getNumBuyers();
                isFirstRound = false;
            }

            if (numBuyers > 1 || (wasAlone && !highestBidder)) {
                if (!hasBid) {
                	if (currentPrice > budget) {
                	    // Terminate buyer agent if current price is higher than budget
                	    MultiAgentAuction.decreaseNumBuyers(); // Decrement the number of buyers
                	    System.out.println("Buyer " + getLocalName() + " cannot bid. Current price is higher than budget.");
                	    doDelete();
                	    return;
                	}
                    double bid = currentPrice + Math.random() * (budget - currentPrice);
                    lastBid = bid; // Update last bid
                    System.out.println("Buyer " + getLocalName() + " bids: " + (int) bid + " DA");

                    // Create and send bid message to the seller agent
                    ACLMessage bidMessage = new ACLMessage(ACLMessage.INFORM);
                    bidMessage.addReceiver(new jade.core.AID("Seller", jade.core.AID.ISLOCALNAME));
                    bidMessage.setContent(String.valueOf(bid));
                    send(bidMessage);

                    hasBid = true;
                }
                // Wait for response from the seller before proceeding
                ACLMessage response = receive();
                if (response != null && response.getContent().startsWith("Current price is")) {
                    // Parse the current price from the message content
                    String[] parts = response.getContent().split(":");
                    if (parts.length == 2) {
                        try {
                            currentPrice = Double.parseDouble(parts[1].trim());
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    hasBid = false; // Reset flag for next bidding round
                    wasAlone = numBuyers == 1; // Check if the buyer was alone in this round
                    highestBidder = lastBid == currentPrice && numBuyers > 1; // Check if the buyer was the highest bidder in the previous round, only if there were more than one buyer
                } else {
                    block();
                }
            } else {
                // If there's only one buyer and they were not alone in the previous round, wait for more buyers to join
                block();
            }
        }
    }
}