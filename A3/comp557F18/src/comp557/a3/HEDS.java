package comp557.a3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * Half edge data structure.
 * Maintains a list of faces (i.e., one half edge of each) to allow
 * for easy display of geometry.
 */
public class HEDS {

    /** List of faces */
    Set<Face> faces = new HashSet<Face>();
    double regParam = 0.01;
    Queue<Edge> queue = new PriorityQueue<Edge> ();
    
    /**
     * Constructs an empty mesh (used when building a mesh with subdivision)
     */
    public HEDS() {
        // do nothing
    }
        
    /**
     * Builds a half edge data structure from the polygon soup   
     * @param soup
     */
    public HEDS( PolygonSoup soup ) {
        faces.clear();
        
        // TODO: Objective 1: create the half edge data structure from the polygon soup    
        ArrayList<Vertex> vertices = (ArrayList<Vertex>) soup.vertexList;
        ArrayList<int[]> fs = (ArrayList<int[]>) soup.faceList;
        
        HashMap<String, HalfEdge> HEdge = new HashMap<String, HalfEdge>();
        
        HalfEdge he = new HalfEdge();
        
        for (int[] face : fs) {
        	HalfEdge[] edges = new HalfEdge[face.length];
        	
        	// connect half-edges surrounding one face
        	for (int i = 0; i < face.length; i++) {
        		he.head = vertices.get(face[i]);
        		if (i > 0) {
        			edges[i-1].next = he;
        		}
        		edges[i] = he;

        		he = new HalfEdge();
        	}
        	edges[face.length-1].next = edges[0];
        	
        	// create and link face to these half-edges
        	Face f = new Face(edges[0]);
        	faces.add(f);
        	for (int i = 0; i < edges.length; i++) {
        		edges[i].leftFace = f;
        		int tail = face[(i + face.length - 1) % face.length];
        		int head = face[i];
        		HEdge.put(tail + "," + head, edges[i]);
        	}
        }
        
        // find twin for each half-edge
        HalfEdge twin = new HalfEdge();
        for (int[] face: fs) {
        	for (int i = 0; i < face.length; i++) {
        		int tail = face[(i + face.length - 1) % face.length];
        		int head = face[i];
        		he = HEdge.get(tail + "," + head);
        		twin = HEdge.get(head + "," +tail);
        		
        		if(he.twin == null) {
        			he.twin = twin;
        			twin.twin = he;
            		he.e = new Edge();
            		he.e.he = he;
            		twin.e = new Edge();
            		twin.e.he = he;
        		}
        		
        		he = new HalfEdge();
        		twin = new HalfEdge();
        	}
        }
        	
        
        
        // TODO: Objective 5: fill your priority queue on load
        buildQueue();
        
    }

    /**
     * You might want to use this to match up half edges... 
     */
    
    
    // TODO: Objective 2, 3, 4, 5: write methods to help with collapse, and for checking topological problems
    private HashSet<HalfEdge> edgeAroundVertex(HalfEdge he) {
    	HashSet<HalfEdge> hs = new HashSet<HalfEdge> ();
    	
    	// get other edges connected to the head of this half-edge
    	HalfEdge current = he.next.twin;
    	do {
    		hs.add(current);
    		current = current.next.twin;
    	} while(current != he);
    	
    	return hs;
    }
    
    public HalfEdge collapse(HalfEdge he) {
    	// return the same half-edge if cannot collapse
    	if (!canCollapse(he)) {
    		return he;
    	}
    	
    	// put he into undo list
    	undoList.add(he);
    	
    	if ( !redoListHalfEdge.isEmpty() ) {
    		redoCollapse();
    	}
    	else {
//    		System.out.println("queue size: " + queue.size());
    		// get other edges connected to head
        	HashSet<HalfEdge> hsh = edgeAroundVertex(he);
        	
        	// get other edges connected to tail
        	
        	HashSet<HalfEdge> hst = edgeAroundVertex(he.twin);
        	
        	hsh.addAll(hst);
        	
        	// get new vertex position after collapse
        	// remove edges from the queue
        	Point3d nVP = collapsePoint(he);
        	Vertex cPoint = new Vertex();
        	cPoint.p = nVP;
        	
        	queue.remove(he.e);
        	queue.remove(he.twin.e);
        	
        	for (HalfEdge h : hsh) {
        		faces.remove(h.leftFace);
        		queue.remove(h.e);
        		queue.remove(h.twin.e);
//        		h.head.p = nVP;
        		h.head = cPoint;
//        		h.head = he.head;
        	}
        	
//        	he.head.p = nVP;
        	
        	he.next.twin.twin = he.next.next.twin;
        	he.next.next.twin.twin = he.next.twin;
        	he.twin.next.twin.twin = he.twin.next.next.twin;
        	he.twin.next.next.twin.twin = he.twin.next.twin;

        	hsh.remove(he.next.next);
        	hsh.remove(he.twin.next.next);

        	// recompute faces for new half-edges
        	for (HalfEdge h : hsh) {
        		Face f = new Face(h);
        		HalfEdge temp = h;
        		do {
        			temp.leftFace = f;
        			temp = temp.next;
        		} while(temp != h);
        		
        		faces.add(f);
        		
        		h.e.he = h;
        		h.twin.e.he = h;
        		
        		h.e.v.set(collapsePoint(h));
    			h.e.v.w = 1;
    			h.twin.e.v = h.e.v;
    			h.e.error = edgeError(h);
    			h.twin.e.error = h.e.error;

        		if (!queue.contains(h.e)){
    				queue.add(h.e);
    			}
        	}
        	
        	vertexQ(he.next.twin);
        	
        	return collapseCandidate();
    	}
    	
    	return he;
    }
    
    private boolean canCollapse(HalfEdge he) {
    	// check if tetrahedron
    	if (faces.size() <= 4) {
    		return false;
    	}
    	
    	// check 1-ring
    	HalfEdge current = he;
    	HashSet<Vertex> vs1 = new HashSet<Vertex>();
    	do {
    		vs1.add(current.twin.head);
    		current = current.next.twin;  		
    	} while(current != he);
    	
    	current = he.twin;
    	HashSet<Vertex> vs2 = new HashSet<Vertex>();
    	do {
    		vs2.add(current.twin.head);
    		current = current.next.twin;  		
    	} while(current != he.twin);
    	
    	int common = 0;
    	for (Vertex v : vs1) {
    		if (vs2.contains(v)) {
    			common++;
    		}
    	}
    	
    	if (common > 2) {
    		return false;
    	}
    	
//    	// check boundary
//    	HashSet<HalfEdge> hsh = edgeAroundVertex(he);
//    	HashSet<HalfEdge> hst = edgeAroundVertex(he.twin);
//    	
//    	boolean ib = false;
//    	boolean jb = false;
//    	
//    	for (HalfEdge h : hsh) {
//    		if(h.twin == null) {
//    			ib = true;
//    		}
//    	}
    	
    	
    	return true;
    }
    
    private Point3d collapsePoint(HalfEdge he) {
    	Point3d head = he.head.p;
    	Point3d tail = he.twin.head.p;
    	Vector3d diff = new Vector3d();
    	diff.sub(head, tail);
    	Point3d mid = new Point3d();
    	mid.scaleAdd(0.5, diff, tail);
    	
    	Matrix4d q = new Matrix4d();
    	q.add(edgeQ(he), qReg(mid));
    	
    	q.setRow(3, 0, 0, 0, 1);
		
		if(q.determinant() == 0.0) {
			return mid;
		}
		
		Matrix4d qinverse =  new Matrix4d();
		qinverse.invert(q);
		
		Vector4d cPoint = new Vector4d();
		qinverse.getColumn(3, cPoint);
    	
    	return new Point3d(cPoint.x, cPoint.y, cPoint.z);
    }
    
    private Matrix4d vertexQ(HalfEdge he) {
    	HashSet<HalfEdge> hs = new HashSet<HalfEdge> ();
    	
    	HalfEdge current = he;
    	do {
    		hs.add(current);
    		current = current.next.twin;	
    	} while(current != he);
    	
    	Matrix4d vQ = new Matrix4d();
    	
    	for (HalfEdge h : hs) {
    		vQ.add(h.leftFace.K);
    	}
    	
    	he.head.Q = vQ;
    	
    	return vQ;
    }
    
    private Matrix4d edgeQ(HalfEdge he) {
    	Matrix4d Qi = he.head.Q;
    	Matrix4d Qj = he.twin.head.Q;
    	
    	Matrix4d eQ = new Matrix4d();
    	eQ.add(Qi, Qj);
    	return eQ;
    }
    
    private Matrix4d qReg(Point3d m) {
    	Matrix4d Qreg = new Matrix4d();
    	Qreg.setIdentity();
    	Qreg.m03 = -m.x;
    	Qreg.m13 = -m.y;
    	Qreg.m23 = -m.z;
    	Qreg.m30 = -m.x;
    	Qreg.m31 = -m.y;
    	Qreg.m32 = -m.z;
    	Qreg.m33 = m.x*m.x+m.y*m.y+m.z*m.z;
    	
    	Qreg.mul(regParam, Qreg);
    	
    	return Qreg;
    }
    
    private void buildQueue() {
    	for (Face f : faces) {
    		HalfEdge he = f.he;
    		HalfEdge current = he;
    		
    		do {
    			vertexQ(current);
    			vertexQ(current.twin);
    			
    			Point3d cPoint = collapsePoint(he);
    			current.e.v.set(cPoint.x, cPoint.y, cPoint.z, 1);
    			current.e.error = edgeError(current);
    			
    			if (!queue.contains(current.e)){
					queue.add(current.e);
				}
				current = current.next;
    		} while(current != he);
    	}
    }
    
    private double edgeError(HalfEdge he) {
    	Vector4d v = he.e.v;
    	Matrix4d q = he.e.Q;
    	Matrix4d temp = new Matrix4d();
    	Matrix4d tempV = new Matrix4d();
    	tempV.setColumn(0, v);
    	
    	temp.mul(q, tempV);
    	double error = v.x * temp.m00 + v.y * temp.m10 + v.z * temp.m20 + v.w * temp.m30;
    	return error;
    }
    
    public HalfEdge collapseCandidate() {
		while((!queue.isEmpty()) && (!canCollapse(queue.peek().he))) {
			queue.remove();
		}
		if(queue.isEmpty()) {
			return faces.iterator().next().he;
		}

		return queue.peek().he;
	}
    
    
    /**
	 * Need to know both verts before the collapse, but this information is actually 
	 * already stored within the excized portion of the half edge data structure.
	 * Thus, we only need to have a half edge (the collapsed half edge) to undo
	 */
	LinkedList<HalfEdge> undoList = new LinkedList<>();
	/**
	 * To redo an undone collapse, we must know which edge to collapse.  We should
	 * likewise reuse the Vertex that was created for the collapse.
	 */
	LinkedList<HalfEdge> redoListHalfEdge = new LinkedList<>();
	LinkedList<Vertex> redoListVertex = new LinkedList<>();

    void undoCollapse() {
    	if ( undoList.isEmpty() ) return; // ignore the request
   
    	HalfEdge he = undoList.removeLast();

    	// TODO: Objective 6: undo the last collapse
    	// be sure to put the information on the redo list so you can redo the collapse too!
    	
    	// put he into redo list
    	redoListHalfEdge.add(he);
    	redoListVertex.add(he.next.twin.head);
    	
    	
    	he.next.twin.twin = he.next;
    	he.next.next.twin.twin = he.next.next;
    	he.twin.next.twin.twin = he.twin.next;
    	he.twin.next.next.twin.twin = he.twin.next.next;
    	
    	
    	// get other edges connected to head
    	HashSet<HalfEdge> hsh = new HashSet<HalfEdge>();
    	
    	HalfEdge current = he.next.twin;
    	do {
    		hsh.add(current);
    		current = current.next.twin;
    	} while(current != he);
    	
    	// get other edges connected to tail
    	HashSet<HalfEdge> hst = new HashSet<HalfEdge>();
    	
    	current = he.twin.next.twin;
    	do {
    		hst.add(current);
    		current = current.next.twin;
    	} while(current != he.twin);
    	
    	// get new vertex position after collapse
    	// remove edges from the queue
    	Vertex head = he.head;
    	Vertex tail = he.twin.head;
    	
    	for (HalfEdge h : hsh) {
    		faces.remove(h.leftFace);
    		h.head = head;
    	}
    	
    	for (HalfEdge h : hst) {
    		faces.remove(h.leftFace);
    		h.head = tail;
    	}
    	
    	
    	hsh.addAll(hst);
    	
    	for (HalfEdge h : hsh) {
    		HalfEdge temp = h;
    		Face f = new Face(temp);
    		do {
    			temp.leftFace = f;
    			temp = temp.next;
    		} while(temp != h);
    		
    		faces.add(f);
    	}
    	
    	
    	
    	
    }
    
    void redoCollapse() {
    	if ( redoListHalfEdge.isEmpty() ) return; // ignore the request
    	
    	HalfEdge he = redoListHalfEdge.removeLast();
    	Vertex v = redoListVertex.removeLast();
    	
    	undoList.add( he );  // put this on the undo list so we can undo this collapse again

    	// TODO: Objective 7: undo the edge collapse!
    	// get other edges connected to head
    	
    	HashSet<HalfEdge> hs = new HashSet<HalfEdge>();
    	
    	HalfEdge current = he.next.twin;
    	do {
    		hs.add(current);
    		current = current.next.twin;
    	} while(current != he);
    	
    	// get other edges connected to tail
    	
    	current = he.twin.next.twin;
    	do {
    		hs.add(current);
    		current = current.next.twin;
    	} while(current != he.twin);
    	
    	// get new vertex position after collapse
    	// remove edges from the queue
    	
    	for (HalfEdge h : hs) {
    		faces.remove(h.leftFace);
    		h.head = v;
    	}
    	
    	
    	he.next.twin.twin = he.next.next.twin;
    	he.next.next.twin.twin = he.next.twin;
    	he.twin.next.twin.twin = he.twin.next.next.twin;
    	he.twin.next.next.twin.twin = he.twin.next.twin;

    	hs.remove(he.next.next);
    	hs.remove(he.twin.next.next);

    	// recompute faces for new half-edges
    	for (HalfEdge h : hs) {
    		Face f = new Face(h);
    		HalfEdge temp = h;
    		do {
    			temp.leftFace = f;
    			temp = temp.next;
    		} while(temp != h);
    		
    		faces.add(f);
    	}
    	
    }
      
    /**
     * Draws the half edge data structure by drawing each of its faces.
     * Per vertex normals are used to draw the smooth surface when available,
     * otherwise a face normal is computed. 
     * @param drawable
     */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // we do not assume triangular faces here        
        Point3d p;
        Vector3d n;        
        for ( Face face : faces ) {
            HalfEdge he = face.he;
            gl.glBegin( GL2.GL_POLYGON );
            n = he.leftFace.n;
            gl.glNormal3d( n.x, n.y, n.z );
            HalfEdge e = he;
            do {
                p = e.head.p;
                gl.glVertex3d( p.x, p.y, p.z );
                e = e.next;
            } while ( e != he );
            gl.glEnd();
        }
    }

}