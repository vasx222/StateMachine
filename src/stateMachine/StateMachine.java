package stateMachine;

import java.io.*;
import java.util.*;

public class StateMachine {
    static class Graph implements Serializable {
        private List<Node> nodes = new ArrayList<>();
        private Set<String> edges = new TreeSet<>();
        private String lambda = "0", initNode = "S";
        private boolean determinized = false;
        private Graph() {}

        private static class Node implements Serializable, Comparable {
            NodeName name;
            List<Edge> edges;
            boolean isFinal;
            boolean used = false;
            Node(NodeName name, List<Edge> edges, boolean isFinal) {
                this.name = new NodeName(name);
                this.edges = edges;
                this.isFinal = isFinal;
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return this.name.equals(((Node)obj).name);
            }

            static int getNodeIndex(NodeName nodeName, List<Node> nodes,
                                    boolean isFinal) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).name.equals(new NodeName(nodeName))) {
                        if (isFinal) {
                            nodes.get(i).isFinal = true;
                        }
                        return i;
                    }
                }
                nodes.add(new Node(new NodeName(nodeName), new LinkedList<>(), isFinal));
                return nodes.size() - 1;
            }

            static void makeUnused(List<Node> nodes) {
                for (Node node : nodes) {
                    node.used = false;
                }
            }

            static void makeEdgesUnused(List<Node> nodes) {
                for (Node node : nodes) {
                    Edge.makeUnused(node.edges);
                }
            }

            @Override
            public String toString() {
                StringBuilder res = new StringBuilder(
                        "Node: " + this.name);
                res.append(this.isFinal ? " [FINAL]\n" : "\n");
                if (edges.size() != 0) {
                    res.append("Edges: ");
                    int i = 0;
                    for (Edge edge : this.edges) {
                        res.append(edge.name).append("=>").
                                append(edge.node.name);
                        if (i++ != this.edges.size() - 1) {
                            res.append(", ");
                        }
                    }
                }
                return res.toString();
            }

            static void printNodes(OutputStream os, List<Node> nodes, boolean considerUse) {
                for (Node node : nodes) {
                    if (!considerUse || node.used) {
                        try {
                            os.write(node.toString().getBytes());
                            os.write("\n\n".getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public int compareTo(Object obj) {
                Node node = (Node)obj;
                return this.hashCode() - node.hashCode();
            }
        }
        private static class Edge implements Serializable {
            String name;
            Node node;
            boolean used = false;
            Edge(String name, Node node) {
                this.name = name;
                this.node = node;
            }

            @Override
            public boolean equals(Object obj) {
                return this.name.equals(((Edge)obj).name) &&
                        this.node.equals(((Edge)obj).node);
            }

            static void makeUnused(List<Edge> edges) {
                for (Edge edge : edges) {
                    edge.used = false;
                }
            }

            static int getEdgeIndex(String edge, String[] edges) {
                for (int i = 0; i < edges.length; i++) {
                    if (edge.equals(edges[i])) {
                        return i;
                    }
                }
                return -1;
            }
        }

        private static class NodeName extends TreeSet<String> implements Serializable {
            @Override
            public boolean equals(Object obj) {
                NodeName nodeName = (NodeName)obj;
                if (this.size() != nodeName.size()) {
                    return false;
                }
                Iterator<String> it1 = this.iterator(), it2 = nodeName.iterator();
                while (it1.hasNext()) {
                    String name1 = it1.next(), name2 = it2.next();
                    if (!name1.equals(name2)) {
                        return false;
                    }
                }
                return true;
            }
            NodeName(String name) {
                super();
                this.add(name);
            }
            NodeName(NodeName nodeName) {
                super();
                this.addAll(nodeName);
            }
        }

        private static class BooleanHolder {
            boolean value;
            BooleanHolder(boolean value) {
                this.value = value;
            }
        }

        private static void simpleBuild(Node node, Node curNode, NodeName[] initOutNodes,
                                        List<Node> nodes, String[] edges, List<Node> tNodes,
                                        int chosenEdge, BooleanHolder isFinal) {
            if (isFinal == null) {
                isFinal = new BooleanHolder(node.isFinal);
            }
            node.used = true;
            NodeName[] outNodes = initOutNodes == null ?
                    new NodeName[edges.length] : initOutNodes;

            for (Edge edge : curNode.edges) {
                if (edge.used) {
                    continue;
                }
                int edgeIndex = Edge.getEdgeIndex(edge.name, edges);
                if (edgeIndex == -1) {
                    if (edge.node.used) {
                        continue;
                    }
                    if (chosenEdge != -1) {
                        if (outNodes[chosenEdge] == null) {
                            outNodes[chosenEdge] = new NodeName(edge.node.name);
                        } else {
                            outNodes[chosenEdge].addAll(edge.node.name);
                        }
                    } else {
                        isFinal.value = isFinal.value || edge.node.isFinal;
                    }
                    simpleBuild(node, edge.node, outNodes, nodes, edges,
                            tNodes, chosenEdge, isFinal);
                } else {
                    if (chosenEdge == -1) {
                        if (outNodes[edgeIndex] == null) {
                            outNodes[edgeIndex] = new NodeName(edge.node.name);
                        } else {
                            outNodes[edgeIndex].addAll(edge.node.name);
                        }
                        edge.used = true;
                        Node.makeUnused(nodes);
                        simpleBuild(node, edge.node, outNodes, nodes, edges,
                                tNodes, edgeIndex, isFinal);
                    }

                }
            }

            if (node.equals(curNode) && chosenEdge == -1) {
                int index1 = Node.getNodeIndex(node.name, tNodes, isFinal.value);
                for (int i = 0; i < edges.length; i++) {
                    if (outNodes[i] != null) {
                        int index2 = Node.getNodeIndex(outNodes[i], tNodes, false);
                        tNodes.get(index1).edges.add(new Edge(edges[i],
                                tNodes.get(index2)));
                    }
                }
            }
        }

        private static void complexBuild(Node node, String[] edges, List<Node> tNodes) {
            NodeName[] outNodes = new NodeName[edges.length];
            boolean isFinal = false;
            node.used = true;

            if (node.name.size() > 1) {
                for (String aName : node.name) {
                    NodeName subName = new NodeName(aName);
                    int index = Node.getNodeIndex(subName, tNodes, false);
                    isFinal = isFinal || tNodes.get(index).isFinal;
                    for (Edge edge : tNodes.get(index).edges) {
                        int edgeIndex = Edge.getEdgeIndex(edge.name, edges);
                        if (outNodes[edgeIndex] == null) {
                            outNodes[edgeIndex] = new NodeName(edge.node.name);
                        } else {
                            outNodes[edgeIndex].addAll(edge.node.name);
                        }
                    }
                }

                int index = Node.getNodeIndex(node.name, tNodes, isFinal);
                for (int i = 0; i < edges.length; i++) {
                    if (outNodes[i] != null) {
                        int index1 = Node.getNodeIndex(outNodes[i], tNodes, false);
                        tNodes.get(index).edges.add(new Edge(edges[i], tNodes.get(index1)));
                    }
                }
            }

            for (Edge edge : node.edges) {
                if (!edge.node.used) {
                    complexBuild(edge.node, edges, tNodes);
                }
            }
        }

        static Graph buildGraph(InputStream is) {
            Graph graph = new Graph();
            try (Scanner scanner = new Scanner(is)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("=")) {
                        String[] symbols = line.split("[ =]+");
                        if (symbols[0].toLowerCase().equals("lambda")) {
                            graph.lambda = symbols[1];
                            continue;
                        }
                        if (symbols[0].toLowerCase().equals("initnode")) {
                            graph.initNode = symbols[1];
                            continue;
                        }
                    }
                    String[] symbols = line.split("[ ]+");
                    int index1 = Node.getNodeIndex(new NodeName(symbols[0]), graph.nodes,
                            symbols.length != 3);
                    if (symbols.length > 2) {
                        int index2 = Node.getNodeIndex(new NodeName(symbols[1]),
                                graph.nodes, false);
                        graph.nodes.get(index1).edges.add(new Edge(symbols[2],
                                graph.nodes.get(index2)));
                        if (!symbols[2].equals(graph.lambda)) {
                            graph.edges.add(symbols[2]);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return graph;
        }
        void determinize(boolean deleteUnused) {
            List<Node> tNodes = new ArrayList<>();
            String[] aEdges = edges.toArray(new String[edges.size()]);

            Node.makeUnused(nodes);
            for (int i = 0; i < nodes.size(); i++) {
                Node.makeEdgesUnused(nodes);
                simpleBuild(nodes.get(i), nodes.get(i),null,
                        nodes, aEdges, tNodes, -1, null);
            }

            Node.makeUnused(nodes);
            int initNodeIndex = Node.getNodeIndex(new NodeName(initNode), tNodes,
                    false);
            complexBuild(tNodes.get(initNodeIndex), aEdges, tNodes);

            if (deleteUnused) {
                List<Node> resNodes = new ArrayList<>();
                for (Node node : tNodes) {
                    if (node.used) {
                        resNodes.add(node);
                    }
                }
                nodes = resNodes;
            } else {
                nodes = tNodes;
            }
            determinized = true;
        }
        void printGraph(OutputStream os) {
            try {
                os.write(("initNode = " + initNode + ", lambda = " + lambda + "\n").
                        getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Node.printNodes(os, this.nodes, false);
        }

        static Graph copy(Graph graph) {
            if (graph == null) {
                return null;
            }
            byte[] arr = null;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(graph);
                arr = os.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Graph res = null;
            ByteArrayInputStream is = new ByteArrayInputStream(arr);
            try (ObjectInputStream ois = new ObjectInputStream(is)) {
                res = (Graph)ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return res;
        }

        void rename(OutputStream os) {
            boolean changed = false;
            for (int i = 0; i < nodes.size(); i++) {
                NodeName oldName = nodes.get(i).name;
                NodeName newName = new NodeName(
                        Character.toString((char)((int)'A' + i)));
                if (!changed && oldName.equals(new NodeName(initNode))) {
                    initNode = newName.first();
                    changed = true;
                }
                nodes.get(i).name = newName;
                if (os != null) {
                    try {
                        os.write((oldName.toString() + " --> " +
                                newName.toString() + "\n").getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private static boolean equalGroup(Node node1, Node node2, List<Elem> groups) {
            if (node1.edges.size() != node2.edges.size()) {
                return false;
            }
            Iterator<Edge> it1 = node1.edges.iterator(),
                    it2 = node2.edges.iterator();
            while (it1.hasNext()) {
                Edge edge1 = it1.next(), edge2 = it2.next();
                if (!edge1.name.equals(edge2.name)) {
                    return false;
                }
                int index1 = getGroupElemNumber(edge1.node, groups),
                        index2 = getGroupElemNumber(edge2.node, groups);
                if (index1 != index2) {
                    return false;
                }
            }
            return true;
        }

        private static int getGroupElemNumber(Node node, List<Elem> groups) {
            int index = -1;
            for (Elem elem : groups) {
                index++;
                if (elem.contains(node)) {
                    return index;
                }
            }
            return -1;
        }

        private static class Elem extends TreeSet<Node> {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder();
                int i = 0;
                for (Node node : this) {
                    s.append(node.name.toString());
                    if (++i != this.size()) {
                        s.append(", ");
                    }
                }
                return s.toString();
            }
        }

        private static class Groups extends LinkedList<Elem> {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder();
                int index = -1;
                for (Elem elem : this) {
                    index++;
                    s.append("[N").append(Integer.toString(index + 1)).append("]: ");
                    s.append(elem.toString());
                    if (index + 1 != this.size()) {
                        s.append("\n");
                    }
                }
                return s.toString();
            }
        }

        static Graph minimize(Graph graph, OutputStream os) {
            try {
                if (!graph.determinized) {
                    throw new Exception("Graph not determinized!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Groups groups = new Groups();
            Elem elem1 = new Elem(), elem2 = new Elem();
            for (Node node : graph.nodes) {
               if (node.isFinal) {
                   elem1.add(node);
               } else {
                   elem2.add(node);
               }
            }
            groups.add(elem1);
            groups.add(elem2);
            try {
                os.write(("[Initial]:\n" + groups.toString() + "\n").
                        getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {
                Groups newGroups = new Groups();
                for (Elem elem : groups) {
                    boolean[] used = new boolean[elem.size()];
                    int index = -1;
                    for (Node node : elem) {
                        if (!used[++index]) {
                            used[index] = true;
                            Elem newElem = new Elem();
                            newElem.add(node);
                            int index2 = -1;
                            for (Node node2 : elem) {
                                if (!used[++index2] && equalGroup(node, node2, groups)) {
                                    newElem.add(node2);
                                    used[index2] = true;
                                }
                            }
                            newGroups.add(newElem);
                        }
                    }
                }
                if (groups.size() == newGroups.size()) {
                    if (os != null) {
                        try {
                            if (newGroups.size() == graph.nodes.size()) {
                                os.write("Impossible to minimize graph!\n".getBytes());
                            } else {
                                os.write(("[Final]:\n" + groups.toString() + "\n").
                                        getBytes());
                                os.write("Minimization successfully completed!\n".
                                        getBytes());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    Graph res = new Graph();
                    res.lambda = graph.lambda;
                    List<Node> newNodes = new ArrayList<>();
                    int index = -1;
                    for (Elem elem : groups) {
                        index++;
                        NodeName name = new NodeName("N" + Integer.toString(index + 1));
                        Node node = new Node(name, new LinkedList<>(), false);

                        NodeName initNodeName = new NodeName(graph.initNode);
                        for (Node elemNode : elem) {
                            if (elemNode.name.equals(initNodeName)) {
                                res.initNode = node.name.first();
                            }
                            if (elemNode.isFinal) {
                                node.isFinal = true;
                            }
                        }
                        newNodes.add(node);
                    }

                    index = -1;
                    for (Elem elem : groups) {
                        index++;
                        Node f = elem.first();
                        for (Edge edge : f.edges) {
                            int index2 = getGroupElemNumber(edge.node, groups);
                            Edge newEdge = new Edge(edge.name, newNodes.get(index2));
                            newNodes.get(index).edges.add(newEdge);
                        }
                    }
                    res.edges = graph.edges;
                    res.nodes = newNodes;

                    return res;
                } else {
                    // next iteration
                    if (os != null) {
                        try {
                            os.write("[Step]:\n".getBytes());
                            os.write((newGroups.toString() + "\n").getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    groups = newGroups;
                }
            } // end while(true)
        }
    }
}
