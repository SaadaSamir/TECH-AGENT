package examples.inter_platform_migration_project;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SellerAgent extends Agent {
    private static final long serialVersionUID = 1L;

    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("seller-service");
        sd.setName(getLocalName() + "-seller-service");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            // System.out.println("Seller-agent " + getAID().getLocalName() + " successfully registered with DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new OfferProposalsBehaviour());
    }

    private class OfferProposalsBehaviour extends CyclicBehaviour {
        private static final long serialVersionUID = 1L;

        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.CFP) {
                    // System.out.println("Received CFP from " + msg.getSender().getLocalName());
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    Map<String, Integer> offer = generateRandomOffer();
                    StringBuilder offerContent = new StringBuilder();
                    for (Map.Entry<String, Integer> criterion : offer.entrySet()) {
                        offerContent.append(criterion.getKey()).append(":").append(criterion.getValue()).append(";");
                    }
                    reply.setContent(offerContent.toString());
                    myAgent.send(reply);
                    System.out.println("Sent offer by " + getAID().getLocalName() + " : " + offerContent.toString());
                }
            } else {
                block();
            }
        }

        private Map<String, Integer> generateRandomOffer() {
            Map<String, Integer> offer = new HashMap<>();
            Random random = new Random();
            offer.put("Price", random.nextInt(100) + 1);
            offer.put("Quality", random.nextInt(100) + 1);
            offer.put("Volume", random.nextInt(1000) + 1);
            offer.put("DeliveryCosts", random.nextInt(50) + 1);
            return offer;
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Seller-agent " + getAID().getLocalName() + " deregistered from DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
