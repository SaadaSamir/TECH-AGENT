package techagentprojet_partie2;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.Random;

public class MainContainer {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");
        AgentContainer mainContainer = rt.createMainContainer(profile);

        try {
            Random rand = new Random();
            int numSellers = rand.nextInt(5) + 3;
            // Create seller agent containers and agents
            for (int i = 1; i <= numSellers; i++) {
                ProfileImpl p = new ProfileImpl();
                p.setParameter(Profile.CONTAINER_NAME, "SellerContainer" + i);
                p.setParameter(Profile.MAIN_HOST, "localhost");
                AgentContainer container = rt.createAgentContainer(p);
                //System.out.println("Container " + i + " created with host: " + host);
                AgentController seller = container.createNewAgent("seller" + i, SellerAgent.class.getName(), new Object[]{});
                seller.start();
            }


            // Create buyer agent after all seller agents have been created
            AgentController buyer = mainContainer.createNewAgent("buyer", BuyerAgent.class.getName(),
                    new Object[]{numSellers});
            System.out.println("Creating " + numSellers + " seller agents for inter-container migration:");
            buyer.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
