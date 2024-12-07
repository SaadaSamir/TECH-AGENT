package techagentprojet_partie2;

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
            System.out.println("Seller-agent " + getAID().getLocalName() + " successfully registered with DF.");
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
                if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("hey")) {
                    // Received greeting from buyer
                    String buyerName = msg.getSender().getLocalName();
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    Map<String, Integer> offer = generateRandomOffer();
                    StringBuilder offerContent = new StringBuilder();
                    for (Map.Entry<String, Integer> criterion : offer.entrySet()) {
                        offerContent.append(criterion.getKey()).append(":").append(criterion.getValue()).append(";");
                    }
                    reply.setContent("hello " + buyerName + " my offer is : " + offerContent.toString());
                    myAgent.send(reply);
                    
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
