package examples.inter_platform_migration_project;

import jade.core.Agent;
import jade.core.PlatformID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import java.util.HashMap;
import java.util.Map;

public class BuyerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private int numSellerAgents;
    private AID[] sellerAgents;
    private String sellerPlatformName = "ams@SellerPlatform:1400/JADE";
    private String sellerPlatformAddress = "http://DESKTOP-NNSU19F:7778/acc";

    protected void setup() {
        // Initialize numSellerAgents from arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            numSellerAgents = (int) args[0];
        } else {
            numSellerAgents = 3; // Default to 3 if no argument is provided
        }

        // Register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("buyer-service");
        sd.setName(getLocalName() + "-buyer-service");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Buyer-agent " + getLocalName() + " successfully registered with DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Find seller agents
        addBehaviour(new OneShotBehaviour() {
            private static final long serialVersionUID = 1L;

            public void action() {
                System.out.println("Searching for seller agents...");
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd1 = new ServiceDescription();
                sd1.setType("seller-service");
                template.addServices(sd1);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    System.out.println("Found the following seller agents:");
                    sellerAgents = new AID[Math.min(result.length, numSellerAgents)];
                    for (int i = 0; i < sellerAgents.length; ++i) {
                        sellerAgents[i] = result[i].getName();
                        System.out.println(sellerAgents[i].getLocalName());
                    }
                    addBehaviour(new MigrateAndEvaluateBehaviour());
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
    }

    private void selectBestOffer() {
        String bestSeller = null;
        int bestValue = 0;
        for (Map.Entry<String, Integer> entry : evaluatedOffers.entrySet()) {
            if (bestSeller == null || entry.getValue() > bestValue) {
                bestSeller = entry.getKey();
                bestValue = entry.getValue();
            }
        }
        System.out.println("Best offer selected by the buyer agent: " + bestSeller + " with a value of " + bestValue);
    }

    private int evaluateOffer(Map<String, Integer> offer) {
        int minPrice = 50;
        int maxQuality = 100;
        int maxVolume = 500;
        int minDeliveryCosts = 10;

        double weightPrice = 0.3;
        double weightQuality = 0.4;
        double weightVolume = 0.2;
        double weightDeliveryCosts = 0.1;

        double normalizedPrice = 1 - (double) (offer.get("Price") - minPrice) / (100 - minPrice);
        double normalizedQuality = (double) (offer.get("Quality") - 0) / (maxQuality - 0);
        double normalizedVolume = (double) (offer.get("Volume") - 0) / (maxVolume - 0);
        double normalizedDeliveryCosts = 1 - (double) (offer.get("DeliveryCosts") - minDeliveryCosts) / (100 - minDeliveryCosts);

        return (int) ((weightPrice * normalizedPrice + weightQuality * normalizedQuality +
                weightVolume * normalizedVolume + weightDeliveryCosts * normalizedDeliveryCosts) * 100);
    }

    private Map<String, Integer> evaluatedOffers = new HashMap<>();

    private class MigrateAndEvaluateBehaviour extends SequentialBehaviour {
        private static final long serialVersionUID = 1L;

        public MigrateAndEvaluateBehaviour() {
            System.out.println("Trying to migrate and evaluate offers...");
            // Add a OneShotBehaviour to perform the migration to the seller platform
            addSubBehaviour(new OneShotBehaviour() {
                private static final long serialVersionUID = 1L;

                public void action() {
                    System.out.println("Migrating to the seller platform...");
                    PlatformID destination = new PlatformID(new AID(sellerPlatformName, AID.ISGUID));
                    destination.setAddress(sellerPlatformAddress);
                    BuyerAgent.this.doMove(destination);
                }
            });

            // Add sub-behaviours for each seller agent
            for (int i = 0; i < sellerAgents.length; i++) {
                final int index = i;
                addSubBehaviour(new OneShotBehaviour() {
                    private static final long serialVersionUID = 1L;

                    public void action() {
                        AID sellerAgent = sellerAgents[index];
                        System.out.println("Buyer: hey " + sellerAgent.getLocalName() + " what's your offer?");
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(sellerAgent);
                        msg.setContent("hey");
                        msg.setConversationId("trade");
                        myAgent.send(msg);
                    }
                });
                addSubBehaviour(new OneShotBehaviour() {
                    private static final long serialVersionUID = 1L;

                    public void action() {
                        ACLMessage reply = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
                        if (reply != null) {
                            String offerContent = reply.getContent();
                            System.out.println("Seller: " + offerContent);
                            String[] criteriaValues = offerContent.split("my offer is : ")[1].split(";");
                            Map<String, Integer> offer = new HashMap<>();
                            for (String criterion : criteriaValues) {
                                String[] parts = criterion.split(":");
                                offer.put(parts[0], Integer.parseInt(parts[1]));
                            }
                            int value = evaluateOffer(offer);
                            System.out.println("Buyer: the calculated value using function is: " + value);
                            evaluatedOffers.put(sellerAgents[index].getLocalName(), value);
                        }
                    }
                });
            }

            // Add a final sub-behaviour to handle the return to the main container and select the best offer
            addSubBehaviour(new OneShotBehaviour() {
                private static final long serialVersionUID = 1L;

                public void action() {
                    System.out.println("Migrating back to the main container.");
                    // Specify the main container location
                    PlatformID home = new PlatformID(new AID("ams@BuyerPlatform:1099/JADE", AID.ISGUID));
                    home.setAddress("http://localhost:1099/acc"); // Use the correct host and port
                    BuyerAgent.this.doMove(home); // Move back to the main container
                }
            });
            addSubBehaviour(new OneShotBehaviour() {
                private static final long serialVersionUID = 1L;

                public void action() {
                    System.out.println("Selecting the best offer after evaluating all offers.");
                    selectBestOffer();
                }
            });
        }
    }

    protected void takeDown() {
        System.out.println("Buyer-agent " + getAID().getLocalName() + " terminating.");
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
