I used hashmap to find half-edges going from vertex i to j, and matched their twins afterwards.
For the collapse part, I checked if the mesh is tetrahedron or if there are more than two common vertices in 1-ring.
It has occured to me that when I was trying to modify the priority queue, removing edge of current half-edge was different from removing edge of its twin.
I thought they should be the same (since I set up as he.e.he = he and he.twin.e.he = he), but I did not know why. So I decide to remove both, if exist.


Boundary bonus: According to notes, 'if {i} and {j} are both boundary vertices, only collapse if {i,j} is a boundary edge'. 
I was not sure how to implement it , but maybe if I keep track of all half-edges when they are created, and find ones with no twins (ones belong to only one polygon),
I can record vertices on these edges as boundary vertices, as well as these edges as boundary edges. 
Then, when I got a half-edge he, if it is already a boundary edge, then it does not matter. If not, and its head and twin.head are in set of boundary vertices, it is not collapsible.
The problem is that for those boundary edges, I will only know one vertex (the head) on it... how can I record the tail vertex without the twin? 