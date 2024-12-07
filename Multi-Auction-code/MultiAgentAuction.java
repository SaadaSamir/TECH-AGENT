package techagentprojet;

import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;
import java.util.List;

public class MultiAgentAuction {
    private static int numBuyers;
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final List<AID> buyerAIDs = new ArrayList<>(); // List to store buyer AIDs

    public static void main(String[] args) {
        numBuyers = (int) (Math.random() * 3) + 4;
        System.out.println("Number of buyer agents: " + numBuyers);

        jade.core.Runtime rt = jade.core.Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.LOCAL_HOST, "localhost");
        p.setParameter(Profile.LOCAL_PORT, "1099");
        //p.setParameter(Profile.GUI, "true");

        ContainerController cc = rt.createMainContainer(p);

        try {
            AgentController sellerAgent = cc.createNewAgent("Seller", SellerAgent.class.getName(), null);
            sellerAgent.start();
            System.out.println("Seller agent created and started.");

            Thread.sleep(1000); // Wait for 1 second

            for (int i = 1; i <= numBuyers; i++) {
                AgentController buyerAgent = cc.createNewAgent("Buyer" + i, BuyerAgent.class.getName(), null);
                buyerAgent.start();
                Thread.sleep(500); // Wait for 0.5 second
            }

            // Release the latch to indicate that all agents are created
            latch.countDown();
        } catch (StaleProxyException | InterruptedException e) {
            System.err.println("Error creating agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static CountDownLatch getLatch() {
        return latch;
    }

    public static int getNumBuyers() {
        return numBuyers;
    }

    public static synchronized void decreaseNumBuyers() {
        numBuyers--;
    }

    // Method to add buyer agent AID to the list
    public static synchronized void addBuyerAID(AID aid) {
        buyerAIDs.add(aid);
    }

    // Method to get the list of buyer agent AIDs
    public static synchronized List<AID> getBuyerAIDs() {
        return new ArrayList<>(buyerAIDs);
    }
}
