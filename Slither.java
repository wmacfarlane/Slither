import java.util.*;
import java.io.*;

public class Slither {
	public static void main (String [] args) throws IOException {
		Scanner sc = new Scanner(System.in);
		System.out.println(introduction());
		String fileName = sc.next();
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String[] line = br.readLine().split(" ");
		int r = Integer.parseInt(line[0]);
		int c = Integer.parseInt(line[1]);
		Board board = new Board(r, c);
		for (int i = 0; i < r; i++) {
			line = br.readLine().split(" ");
			for (int j = 0; j < c; j++) {
				board.lineReqs[i][j] = new LineReq(Integer.parseInt(line[j]));
			}
		}
		br.close();
		slither(sc, board);
	}
	static void slither(Scanner sc, Board board) {
		System.out.println(instructions());
		BoardLogic boardLogic = new BoardLogic(board);
		while (true) {
			System.out.print(board);
			if (boardLogic.solved()) {
				System.out.println("Congratulations! You solved it!");
				break;
			}
			int r = sc.nextInt() - 1;
			int c = sc.nextInt() - 1;
			Side side = Side.translate(sc.next().charAt(0));
			board.mark(r, c, side);
		}
	}
	static String instructions() {
		return 
		"\n<Instructions>\n" + 
		"Type \"N N L\", where the first two Ns represent the row and column " + 
		"of the cell whose adjacent wall you wish to fill in and L represents the side " + 
		"of the cell where you'd like to put the wall " + 
		"(T for top, B for bottom, L for left, and R for right).\n" +
		"</Instructions>\n";
	}
	static String introduction() {
		String s = 
		"Welcome to Slither!\n\nSlither is a puzzle game. " + 
		"If you can create a continuous loop that adheres to " + 
		"the numbered requirement squares seen on the board, " + 
		"you win!\n\n";
		s += 
		"For instance, the following is a winning solution because it has " + 
		"as many lines around each square as the number specified " +
		"in the center of that square.\n\n" +
		"It also contains exactly one continuous loop.\n\n";
		s +=
		"    1 2 \n" +
		"        \n" +
		"   +-+-+\n" + 
		"1  |3 3|\n" +
		"   +-+-+\n" +
		"2       \n" +
		"   + + +\n\n";

		s += "Please enter the name of the input file (e.g., \"Slither.in\").";
		return s;
	}
}
// This class represents the numerical requirement for the number of lines
// to border a particular cell.
class LineReq {
	private int value;
	public LineReq(int v) {
		value = v;
	}
	public boolean hasValue() {
		return value != -1;
	}
	public int getValue() {
		return value;
	}
}
class BoardLogic {
	Board board;
	int r, c;
	BoardLogic(Board board) {
		this.board = board;
		this.r = board.r;
		this.c = board.c;
	}
	/*
		There are two requirements that a solved puzzle must satisfy.
		1) A single looped path has been generated, with no crosses or branches
		2) Each line requirement must be met.
		
		My solution for (1) is:
			for each end of a 1-unit line segment,
				add both corners of the segment to a list of corners.
		A list in which each corner appears 0 or 2 times guarantees
		that we have a connected loop without crosses or branches.
	*/
	public boolean solved() {
		return rightNumberOfCorners() && metLineRequirements() && justOneLoop();
	}
	private boolean rightNumberOfCorners() {
		int[][] corners = new int[r+1][c+1];
		// The value at each cell corresponds to the number of times the top-left
		// corner of that cell layed at the end of a 1-unit line segment.
		for (int i = 0; i <= r; i++) {
			for (int j = 0; j < c; j++) {
				if (board.isMarked(i, j, Side.TOP)) {
					corners[i][j]++;
					corners[i][j+1]++;
				}
			}
		}
		for (int i = 0; i < r; i++) {
			for (int j = 0; j <= c; j++) {
				if (board.isMarked(i, j, Side.LEFT)) {
					corners[i][j]++;
					corners[i+1][j]++;
				}
			}
		}
		// Now let's count our corners.
		for (int i = 0; i <= r; i++) {
			for (int j = 0; j <= c; j++) {
				if (corners[i][j] != 0 && corners[i][j] != 2)
					return false;
			}
		}
		return true;
	}
	private boolean metLineRequirements() {
		// Now let's make sure we've met the line requirements.
		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				if (!board.lineReqs[i][j].hasValue())
					continue;
				int target = board.lineReqs[i][j].getValue();
				int count = 0;
				for (Side side : Side.values()) {
					if (board.isMarked(i, j, side))
						count++;
				}
				if (target != count)
					return false;
			}
		}
		return true;
	}
	private boolean justOneLoop() {
		Trio t = null;
		SEARCH:
		for (int i = 0; i <= board.r; i++) {
			for (int j = 0; j <= board.c; j++) {
				for (Side s : Side.RELEVANT_SIDES) {
					if (board.isMarked(i, j, s)) {
						t = new Trio(i, j, s);
						break SEARCH;
					}
				}
			}
		}
		if (t == null) {
			return false;
		}
		HashSet<Trio> hs = new HashSet<Trio>();
		LinkedList<Trio> q = new LinkedList<Trio>();
		q.offer(t);
		hs.add(t);
		while(!q.isEmpty()) {
			Trio u = q.poll();
			Iterator<Trio> it = board.getTouchingWalls(u).iterator();
			while (it.hasNext()) {
				Trio v = it.next();
				if (!hs.contains(v)) {
					hs.add(v);
					q.add(v);
				}
			}
		}
		for (int i = 0; i <= board.r; i++) {
			for (int j = 0; j <= board.c; j++) {
				for (Side s : Side.RELEVANT_SIDES) {
					if (board.isMarked(i, j, s) && !hs.contains(new Trio(i, j, s))) {
						return false;
					}
				}
			}
		}
		return true; // no line was found that wasn't touching the main loop
	}
}
class Trio {
	int i;
	int j;
	Side side;
	@Override
	public int hashCode() {
		return i + (j * 26) + (side.ordinal() * 806); //i <= 25, j <= 30
	}
	public Trio (int a, int b, Side c) {
		i = a;
		j = b;
		side = c;
	}
	public String toString() {
		return "i: " + i + "j: " + j + "side: " + side;
	}
	@Override
	public boolean equals(Object o) {
		return hashCode() == o.hashCode();
	}
}
class Board {
	int r, c;
	private Wall[][] walls;
	LineReq[][] lineReqs;
	public Board (int r, int c) {
		this.r = r;
		this.c = c;
		walls = new Wall[r][c];
		lineReqs = new LineReq[r][c];
		for (int i = 0; i < r; i++)
			for (int j = 0; j < c; j++)
				walls[i][j] = new Wall();
	}
	public boolean isMarked(int i, int j, Side side) {
		Trio equivWall = getEquivalentWall(i, j, side);
		int equivI = equivWall.i;
		int equivJ = equivWall.j;
		Side correspondingSide = equivWall.side;
		if (rowInBounds(i) && columnInBounds(j))
			return walls[i][j].isMarked(side);
		if (rowInBounds(equivI) && columnInBounds(equivJ)) // if they specify the left of the rightmost wall
			return walls[equivI][equivJ].isMarked(correspondingSide);
		return false;
	}
	public List<Trio> getTouchingWalls(Trio t) {
		List<Trio> touchingWalls = new ArrayList<Trio>();
		Trio base;
		if (t.side == Side.BOTTOM || t.side == Side.RIGHT) {
			base = getEquivalentWall(t.i, t.j, t.side);
		} else {
			base = t;
		}
		switch (base.side) {
			case TOP:
				for (int c = base.j - 1; c <= base.j + 1; c++) {
					if (isMarked(base.i, c, Side.TOP)) {
						touchingWalls.add(new Trio(base.i, c, Side.TOP));
					}
				}
				for (int r = base.i - 1; r <= base.i; r++) {
					for (int c = base.j; c <= base.j + 1; c++) {
						if (isMarked(r, c, Side.LEFT)) {
							touchingWalls.add(new Trio(r, c, Side.LEFT));
						}
					}
				}
			break;
			case LEFT:
				for (int r = base.i - 1; r <= base.i+1; r++) {
					if (isMarked(r, base.j, Side.LEFT)) {
						touchingWalls.add(new Trio(r, base.j, Side.LEFT));
					}
				}
				for (int r = base.i; r <= base.i + 1; r++) {
					for (int c = base.j - 1; c <= base.j; c++) {
						if (isMarked(r, c, Side.TOP)) {
							touchingWalls.add(new Trio(r, c, Side.TOP));
						}
					}
				}
			break;
			default:
				throw new RuntimeException("GetTouchingWalls issues");
		}
		return touchingWalls;
	}
	public Trio getEquivalentWall(int i, int j, Side side) {
		int equivI, equivJ;
		Side correspondingSide;
		if (side == Side.TOP) {
			equivI = i - 1;
			equivJ = j;
			correspondingSide = Side.BOTTOM;
		} else if (side == Side.RIGHT) {
			equivI = i;
			equivJ = j + 1;
			correspondingSide = Side.LEFT;
		} else if (side == Side.BOTTOM) {
			equivI = i + 1;
			equivJ = j;
			correspondingSide = Side.TOP;
		} else { // left
			equivI = i;
			equivJ = j - 1;
			correspondingSide = Side.RIGHT;
		}
		return new Trio(equivI, equivJ, correspondingSide);
	}
	// Invariant: mark(i, j, side) must be reflected on both sides of the wall
	public void mark(int i, int j, Side side) {
		Trio equivWall = getEquivalentWall(i, j, side);
		int equivI = equivWall.i;
		int equivJ = equivWall.j;
		Side correspondingSide = equivWall.side;

		if (rowInBounds(i) && columnInBounds(j)) {
			walls[i][j].mark(side);
		}
		if (rowInBounds(equivI) && columnInBounds(equivJ)) {
			walls[equivI][equivJ].mark(correspondingSide);
		}
	}
	public boolean rowInBounds(int i) {
		return (i >= 0) && (i < r);
	}
	public boolean columnInBounds(int j) {
		return (j >= 0) && (j < c);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("   ");
		for (int j = 1; j <= c; j++) {
			sb.append(" " + (j % 10));
		}
		sb.append("\n\n");
		sb.append("   ");
		addWallsToString(sb, 0, Side.TOP);
		for (int i = 0; i < r; i++) {
			sb.append((i+1) + " ");
			if (i+1 < 10)
				sb.append(" ");
			addContentsToString(sb, i);
			sb.append("   ");
			addWallsToString(sb, i, Side.BOTTOM);
		}
		return sb.toString();
	}
	private void addContentsToString(StringBuilder sb, int row) {
		if (isMarked(row, 0, Side.LEFT)) {
			sb.append("|");
		} else {
			sb.append(" ");
		}
		for (int j = 0; j < c; j++) {
			// square marking
			if (lineReqs[row][j].hasValue()) {
				sb.append(lineReqs[row][j].getValue());
			} else {
				sb.append(" ");
			}
			// wall marking
			if (isMarked(row, j, Side.RIGHT)) {
				sb.append("|");
			} else {
				sb.append(" ");
			}
		}
		sb.append("\n");
		return;
	}
	private void addWallsToString(StringBuilder sb, int row, Side side) {
		for (int j = 0; j < c; j++) {
			sb.append("+");
			if (isMarked(row, j, side)) {
				sb.append("-");
			} else {
				sb.append(" ");
			}
		}
		sb.append("+\n");
	}
	private class Wall {
		int value; // represented by a 4-digit base 2 value
		public Wall() {
			value = 0;
		}
		public void mark (Side side) {
			if (!isMarked(side)) {
				value += (1 << side.ordinal());
			}
		}
		public boolean isMarked (Side side) {
			return (value & (1 << side.ordinal())) != 0;
		}
	}
}
enum Side {
	TOP, RIGHT, BOTTOM, LEFT;
	public static final Side[] RELEVANT_SIDES = {Side.TOP, Side.LEFT};
	static Side translate (char c) {
		switch (c) {
			case 't':
			case 'T':
				return Side.TOP;
			case 'r':
			case 'R':
				return Side.RIGHT;
			case 'b':
			case 'B':
				return Side.BOTTOM;
			case 'l':
			case 'L':
				return Side.LEFT;
			default:
				throw new IllegalArgumentException(
					"You incorrectly specified the side that you wanted to mark!"
				);
		}
	}
}