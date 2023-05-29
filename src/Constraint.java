import java.util.*;
import java.io.*;

/**
 *  The {@code Constraint} class represents a LP's individual constraint with its id,
 *  coefficients, type (i.e., =, >=, or <=), and RHS.
 *
 *  It supports the following: getters getId, getType, getRHS, getCoeffs, getCoeff for
 *  retrieving the constraint's id, type, RHS, coefficients, and a single coefficient;
 *  setters: setId, setType, setRHS for setting the constraint's id, type, and RHS,
 *  respectively; an auxiliary method addCoeff for adding a new constraint coefficient
 *  for a specified variable.
 *
 *  @author Dimitris Papachristoudis
 */
public class Constraint
{
	private int id;					// Each Constraint is assigned a unique id

	// private TreeMap<String, Double> coeffs;	// A TreeMap containing the variable
							// coefficients for this Constraint instance

	private HashMap<String, Double> coeffs;		// A HashMap containing the variable
							// coefficients for this Constraint instance

	private byte type;   // An integer number indicating the type
			     // of relation in the Constraint:
			     //-1: represents <=
			     // 0: represents ==
			     // 1: represents >=

	private double RHS;  // The constraint's right hand side number

	// Constructor#1
	public Constraint(int id, byte type)
	{
		this.id = id;
		this.type = type;
		// coeffs = new TreeMap<String, Double>();
		coeffs = new HashMap<String, Double>();
		RHS = 0;
	}

	// Constructor#2
	public Constraint(int id, byte type, double rhs)
	{
		this.id = id;
		this.type = type;
		// coeffs = new TreeMap<String, Double>();
		coeffs = new HashMap<String, Double>();
		RHS = rhs;
	}

	// Retrieve the Constraint's unique id
	public int getId()
	{
		return id;
	}

	// Set the Constraint's id to a given one
	public void setId(int newId)
	{
		id = newId;
	}

	// Retrieve the Constraint's type, ie: equality or inequality (see above)
	public byte getType()
	{
		return type;
	}

	// Set the Constraint's type to a given one
	public void setType(byte newType)
	{
		if (newType == -1 || newType == 0 || newType == 1)
			type = newType;
	}

	// Retrieve the Constraint's right hand side value
	public double getRHS()
	{
		return RHS;
	}

	// Set the Constraint's right hand side to a specified value
	public void setRHS(double x)
	{
		RHS = x;
	}

	// Retrieve the HashMap containing the
	// Constraint's coefficients for each variable
	public HashMap<String, Double> getCoeffs()
	{
		return coeffs;
	}

	// Add a new <variable, value> pair to the HashMap
	public void addCoeff(String varName, double value)
	{
		coeffs.put(varName, Double.valueOf(value));
	}

	// A method for retrieving a given variable's coefficient
	// in the current constraint. Returns zero if not found or
	// if the specified variable name is null.
	public double getCoeff(String varName)
	{
		try
		{
			Double coeff = coeffs.get(varName);
			if (coeff != null)
				return coeff.doubleValue();
			else
				return 0;
		}
		catch (Exception e)
		{
			System.out.printf("ERROR: An error occured while attempting to retrieve the coefficient for variable: %s!%n", varName);
			e.printStackTrace();
			return 0;
		}
	}


	// Driver method for testing purposes
	public static void main(String args[]) throws IOException
	{}

}
