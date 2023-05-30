/******************************************************************************
 *  Compilation:  javac Converter.java
 *  Execution:    java Converter input.mps
 *  Dependencies: -
 *  Data files:   ADLITTLE.mps
 *                AFIRO.mps
 *                BNL1.mps
 *                FFFFF800.mps
 *
 *  A converter for .mps files, implemented using HashMaps.
 *
 ******************************************************************************/

import java.io.*;
import java.util.*;

/**
 *  The {@code Converter} class represents a converter object that receives an .mps file
 *  as input, parses it, and loads its contents into data structures (arrays) that enable
 *  them to be used for solving Linear Programming Problems (for example, Simplex).
 *
 *  It supports the following primary operations: convert which performs the parsing, as
 *  well as populating appropriate data structures (i.e., arrays), isConverted which
 *  indicates if the input file has been converted or not, getA which returns the linear
 *  problem's <em>A</em>-matrix, getRHS which returns the right-hand-side vector(s),
 *  getEqin which returns a vector indicating the relation (i.e., =, >=, or <=) between
 *  each constraint row and its associated RHS, getObjFunctionCoeffs which returns the
 *  coefficients of the objective function, getMinMax which returns a byte indicating
 *  whether the linear problem is a minimization or a maximization problem, getObfName
 *  which returns the objective function's name, getNumberOfConstraints which returns the
 *  number of constraints in the problem, getNumberOfVariables which returns the number of
 *  variables in the linear problem, getConstraintNames which returns the set of names
 *  for the constraints, and getVariableNames which returns the set of names for the
 *  variables.
 *
 *  @author Dimitris Papachristoudis
 */
public class Converter
{
	// Enumeration State represents the converter's states as it parses
	// the input .mps file.
	private static enum State
	{
		START,
		READ_NAME,
		READ_ROWS,
		READ_COLS,
		READ_COL_DATA,
		READ_RHS,
		READ_ENDDATA,
		INVALID
	}
	
	/*
	OLD
	private static final int START = 0;
	private static final int READ_NAME = 1;
	private static final int READ_ROWS = 2;
	private static final int READ_COLS = 3;
	private static final int READ_COL_DATA = 4;
	private static final int READ_RHS = 5;
	private static final int READ_ENDDATA = 6;
	private static final int INVALID = 7;
	*/

	// A variable indicating the converter's current
	// state while parsing the input file.
	private State currentState;
	
	// The file for which we create the converter instance
	private File f;

	private BufferedReader br;

	// TreeMap's implementation provides guaranteed O(log(n)) time cost for the containsKey, get, put and remove operations.
	// private TreeMap<String, Integer> varNames;
	// private TreeMap<String, Constraint> constraints;
	// private TreeMap<String, Double> obFunctionCoeffs;

	// HashMap provides constant-time O(1) performance for the basic operations (get and put).
	private HashMap<String, Integer> varNames;
	private HashMap<String, Constraint> constraints;
	private HashMap<String, Double> obFunctionCoeffs;

	private boolean isConverted;    // Boolean value indicating whether conversion was successful or not
	private int m;	        	// Number of constraints
	private int n;       	  	// Number of variables
	private byte minmax;    	// -1 for minimization problems or 1 for maximization problems
	private String obfName;	 	// The objective function's name 
	private double[][] A;   	// Array of coefficients
	private byte[] Eqin;    	// A vector indicating equality for each constraint:
					// -1: indicates <=
					// 0: indicates ==
					// 1: indicates >=
	private double[] b;	   	// A vector containing the RHS' for each constraint
	private double[] C;     	// Objective function coefficients


	/**
     	* Initializes a new Converter for a specified file.
     	*
     	* @param  f the file for which we wish to perform the conversion
     	*/
	public Converter(File f)
	{
		this.f = f;

		// Open file for reading data
		try
		{
			br = new BufferedReader(new FileReader(f));
			
		}
		catch (FileNotFoundException ex)   // File could not be opened (eg: not found or corrupt)
		{
			System.out.printf("ERROR: File: %s could not be opened!!!%n", f.getName());
			ex.printStackTrace();
			System.exit(1);
		}

		currentState = State.START;

		// OLD (using TreeMap)
		// constraints = new TreeMap<String, Constraint>();
		// varNames = new TreeMap<String, Integer>();
		// obFunctionCoeffs = new TreeMap<String, Double>();

		constraints = new HashMap<String, Constraint>();
		varNames = new HashMap<String, Integer>();
		obFunctionCoeffs = new HashMap<String, Double>();

		m = 0;
		n = 0;
		isConverted = false;
	}

	/**
     	* Parses the file and loads its data into data structures.
     	*
     	*/
	public void convert()
	{
		// Some handy patterns for pattern matching while reading from the file
		String WORD = "[\\w\\.]+";
		String SPACE = "[\\s]+";
		String REALNUMBER = "[\\+\\-]?([0-9]+([\\.][0-9]*)?|[\\.][0-9]+)";

		// Some patterns for pattern matching in the .mps file
		String EMPTYROW = "^[\\s]*$";
		String NAMEROW = "^[\\s]*[nN][aA][mM][eE]" + SPACE + WORD + "(" + SPACE + "[(][mM][aA][xX][)]|[(][mM][iI][nN][)])?[\\s]*$";
		String ROWS = "^[\\s]*[rR][oO][wW][sS][\\s]*$";
		String OBJNAME = "^[\\s]*[nN]" + SPACE + WORD + "[\\s]*$";
		String EQ_RELATIONS = "^[\\s]*[ELG]" + SPACE + WORD + "[\\s]*$";
		String COLUMNS = "^[\\s]*[cC][oO][lL][uU][mM][nN][sS][\\s]*$";
		String COLUMNDATA = "^[\\s]*" + WORD + SPACE + WORD + SPACE + REALNUMBER + "(" + SPACE + WORD + SPACE + REALNUMBER + ")?[\\s]*$";
		String RHS = "^[\\s]*[rR][hH][sS][\\s]*$";
		String RHSDATA = "^[\\s]*" + WORD + SPACE + WORD + SPACE + REALNUMBER + "(" + SPACE + WORD + SPACE + REALNUMBER + ")?[\\s]*$";
		String ENDDATA = "^[\\s]*[eE][nN][dD][aA][tT][aA][\\s]*$";

		StringTokenizer tokenizer;

		int lineNumber = 0;
		String line = null;

		// Read file contents
		try
		{
			// For each line and as long as the file structure is valid
			while ((line = br.readLine()) != null && (currentState != State.INVALID))
			{
				// Increment current line number
				lineNumber++;

				// If the current line is not an all whitespace row
				if (!line.matches(EMPTYROW))
				{
					tokenizer = new StringTokenizer(line);
					if (currentState == State.START)
					{
						if (line.matches(NAMEROW))
						{
							if (line.contains("MAX"))
								minmax = 1;
							else
								minmax = -1;
							currentState = State.READ_NAME;
						}
						else
							currentState = State.INVALID;
					}
					else if (currentState == State.READ_NAME)
					{
						if (line.matches(ROWS))
							currentState = State.READ_ROWS;
						else
							currentState = State.INVALID;
					}
					else if (currentState == State.READ_ROWS)
					{
						if (line.matches(EQ_RELATIONS))
						{
							String typeString = tokenizer.nextToken(),
							       constraintName = tokenizer.nextToken();
							byte type;

							if (typeString.equals("E"))
								type = 0;
							else if (typeString.equals("L"))
								type = -1;
							else   // typeString.equals("G")
								type = 1;

							// Create a new Constraint entry and increment the constraints counter (m)
							constraints.put(constraintName, new Constraint(m, type));

							// Increment number of constraints
							m++;

							// Note that we remain at the very same state (READ_ROWS)
							// currentState = State.READ_ROWS;
						}
						else if (line.matches(OBJNAME))
						{
							// If the objective function's name has not been initialized
							if (obfName == null)
							{
								tokenizer.nextToken();
								obfName = tokenizer.nextToken();
								currentState = State.READ_ROWS;
							}
							else   // obfName != null
								currentState = State.INVALID;
						}
						else if (line.matches(COLUMNS))
							currentState = State.READ_COLS;
						else
							currentState = State.INVALID;
					}
					else if (currentState == State.READ_COLS)
					{
						if (line.matches(COLUMNDATA))
						{
							String varName = tokenizer.nextToken();

							// If current variable has not been met already
							if (varNames.get(varName) == null)
							{
								varNames.put(varName, Integer.valueOf(n));
								n++;
							}

							String  constraintName,
								coefficient;
							while (tokenizer.hasMoreTokens())
							{
								constraintName = tokenizer.nextToken();
								coefficient = tokenizer.nextToken();
								
								if (constraintName.equals(obfName))
									obFunctionCoeffs.put(varName, Double.parseDouble(coefficient));
								else
									constraints.get(constraintName).addCoeff(varName, Double.parseDouble(coefficient));   
							}
							
							// Note that we remain at the very same state (READ_COLS)
							// currentState = State.READ_COLS;
						}
						else if (line.matches(RHS))
							currentState = State.READ_RHS;
						else
							currentState = State.INVALID;
					}
					else if (currentState == State.READ_RHS)
					{
						if (line.matches(RHSDATA))
						{
							tokenizer.nextToken();

							String  constraintName, coefficient;

							while (tokenizer.hasMoreTokens())
							{
								constraintName = tokenizer.nextToken();
								coefficient = tokenizer.nextToken();

								Constraint c = constraints.get(constraintName);

								// If a corresponding constraint exists
								if (c != null)
									c.setRHS(Double.parseDouble(coefficient));
								else	// c == null
									currentState = State.INVALID;
							}

							// Note that we remain at the very same state (READ_RHS)
							// currentState = State.READ_RHS;

						}
						else if (line.matches(ENDDATA))
							currentState = State.READ_ENDDATA;
						else
							currentState = State.INVALID;
					}
					else if (currentState == State.READ_ENDDATA)
						currentState = State.INVALID;						
					else   // Will never execute
					{}

				}
			}


			// If the current state is the INVALID state,
			// an error has occured while parsing the file
			if (currentState == State.INVALID)
			{
				System.err.printf("An error occured while parsing the input file!!!%nLine Number: %d%n", lineNumber);
				System.err.println("There's something wrong with this line:");
				System.err.printf("\"%s\"%n", line);
			}
			else   // Parsing was successful
			{
				System.out.printf("Parsing %s complete!%n", f.getName());
				A = new double[m][n];
				Eqin = new byte[m];
				C = new double[n];
				b = new double[m];

				// Transfer data
				
				// For each variable
				for (Map.Entry<String, Integer> var : varNames.entrySet())
				{
					int varId = var.getValue().intValue();

					// Retrieve coefficients for the Objective Function
					Double temp = obFunctionCoeffs.get(var.getKey());
					if (temp == null)
						C[varId] = 0;
					else
						C[varId] = temp.doubleValue();
					

					// For each constraint
					for (Map.Entry<String, Constraint> con : constraints.entrySet())
					{
						Constraint current = con.getValue();
						int conId = current.getId();

						A[conId][varId] = current.getCoeff(var.getKey());

						Eqin[conId] = current.getType();
						b[conId] = current.getRHS();
					}
				}

				// Conversion was a success!!!
				isConverted = true;
			}
			
		}
		catch (IOException ex)
		{
			System.err.println("ERROR: An error occured!");
			ex.printStackTrace();
		}
	}
	
	/**
     	* A method for checking whether the conversion was successful or not.
     	*
     	* @return true iff the file has been successfully converted,
        * and false otherwise.
     	*/
	public boolean isConverted()
	{
		return isConverted;
	}
	
	/**
     	* A method for retrieving the <em>A</em> matrix of the LP's coefficients.
     	*
     	* @return the <em>A</em> matrix of the LP's coefficients, or null if the
        * file has not been parsed yet.
     	*/
	public double[][] getA()
	{
		if (isConverted)
			return A;
		else
			return null;
	}
	
	/**
     	* A method for retrieving the RHS vector of the LP.
     	*
     	* @return the RHS vector of the LP, or null if the
        * file has not been parsed yet.
     	*/
	public double[] getRHS()
	{
		if (isConverted)
			return b;
		else
			return null;
	}

	/**
     	* A method for retrieving the relations vector of the LP,
        * specifying whether each constraint is of type =, >=, or <=.
     	*
     	* @return the relations vector of the LP, or null if the
        * file has not been parsed yet.
     	*/
	public byte[] getEqin()
	{
		if (isConverted)
			return Eqin;
		else
			return null;
	}
	
	/**
     	* A method for retrieving the objective function's coefficients.
     	*
     	* @return the vector of the LP's objective function coefficients,
        * or null if the file has not been parsed yet.
     	*/
	public double[] getObjFunctionCoeffs()
	{
		if (isConverted)
			return C;
		else
			return null;
	}
	
	/**
     	* A method for retrieving the objective function's coefficients.
     	*
     	* @return -1 for minimization problems, 1 for maximization problems,
	* 0 if the file has not been parsed.
     	*/
	public byte getMinMax()
	{
		if (isConverted)
			return minmax;
		else
			return 0;
	}

	/**
     	* A method for retrieving the objective function's name.
     	*
     	* @return the objective function name if the file has been
        * parsed successfully.
     	*/
	public String getObfName()
	{
		if (isConverted)
			return obfName;
		else
			return null;
	}
	
	/**
     	* A method for retrieving the LP's number of constraints.
     	*
     	* @return number of the LP's constraints, or 0 if the file
        * has not been parsed.
     	*/
	public int getNumberOfConstraints()
	{
		if (isConverted)
			return m;
		else
			return 0;
	}

	/**
     	* A method for retrieving the LP's number of variables.
     	*
     	* @return number of the LP's variables, or 0 if the file
        * has not been parsed.
     	*/
	public int getNumberOfVariables()
	{
		if (isConverted)
			return n;
		else
			return 0;
	}

	/**
     	* A method for retrieving the LP's constraint names.
     	*
     	* @return the set of names for the LP's constraints.
     	*/
	public Set<String> getConstraintNames()
	{
		return constraints.keySet();
	}

	/**
     	* A method for retrieving the LP's variable names.
     	*
     	* @return the set of names for the LP's variables.
     	*/
	public Set<String> getVariableNames()
	{
		return varNames.keySet();
	}

	/**
     	* Unit tests the {@code Converter} class.
     	*
     	* @param args the command-line arguments
        * @throws IOException if parsing from the input file fails
     	*/
	public static void main(String args[])
	{
		final boolean verbose = false;

		String fileName = "";
		try
		{
			fileName = args[0];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.err.println("Usage of class Converter:\n% java Converter <file name>.mps");
			System.exit(2);
		}
		if (fileName.matches("^[\\w]+[\\.]mps$"))
		{
			File myFile = new File(".\\" + fileName);
			if (myFile.exists())
			{
				System.out.printf("File: %s found.%n", fileName);
				if (myFile.canRead())
				{
					System.out.printf("File: %s is read enabled.%n", fileName);
					Converter myConverter = new Converter(myFile);
					long startTime, stopTime;   // Variables for timing the elapsed time
								    // required in order to perform the conversion.
					
					// Start timer
					startTime = System.currentTimeMillis();
					
					// Perform conversion
					myConverter.convert();
					
					// Stop timer
					stopTime = System.currentTimeMillis();
					
					long elapsedTime = stopTime - startTime;

					if (myConverter.isConverted())
					{
						System.out.println("Conversion successful!");
						System.out.printf("Elapsed time: %d ms.%n", elapsedTime);
						
						// Display converted info
						int m = myConverter.getNumberOfConstraints();
						int n = myConverter.getNumberOfVariables();
						double[][] A = myConverter.getA();
						byte[] Eqin = myConverter.getEqin();
						double[] b = myConverter.getRHS();
						double[] objFunction = myConverter.getObjFunctionCoeffs();
						String obfName = myConverter.getObfName();
						String type = (myConverter.getMinMax() == -1? "min": "max");

						// Display results

						System.out.println("----------------------------------------");
						System.out.println("Problem Type: " + type);
						System.out.println("Objective Function Name: " + obfName);
						System.out.println("Number of Constraints: " + m);
						System.out.println("Number of Variables: " + n);

						// The following code may produce a substantially huge output size
						// for some benchmarks, and is thus omitted by default.
						if (verbose)
						{
							System.out.println("Objective Function coefficients:");
							for (int i=0; i<n; i++)
								System.out.print(objFunction[i] + "\t");
							System.out.println();
							System.out.println("Array of constraint coefficients:");
							for (int i=0; i<m; i++)
							{
								for (int j=0; j<n; j++)
									System.out.print(A[i][j] + "\t\t");
								System.out.println();
							}
							System.out.println("Eqin vector:");
							for (int i=0; i<m; i++)
								System.out.print(Eqin[i] + "\t");
							System.out.println();
							System.out.println("Vector of constraint RHSs:");
							for (int i=0; i<m; i++)
								System.out.print(b[i] + "\t");
							System.out.println();
							System.out.println("Constraint names:");
							System.out.println(myConverter.getConstraintNames());
							System.out.println("Variable names:");
							System.out.println(myConverter.getVariableNames());
						}

						System.out.println("----------------------------------------");
					}
					
				}
				else
					System.err.printf("File: %s is not read enabled!%n", fileName);
			}
			else
				System.err.printf("File: %s does not exist!%n", fileName);
		}
		else
			System.err.println("Not an .mps file!");
	}

}
