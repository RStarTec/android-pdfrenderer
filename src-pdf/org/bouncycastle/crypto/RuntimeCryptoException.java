package org.bouncycastle.crypto;

/**
 * the foundation class for the exceptions thrown by the crypto packages.
 */
public class RuntimeCryptoException 
    extends RuntimeException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 7991303447252124307L;

	/**
     * base constructor.
     */
    public RuntimeCryptoException()
    {
    }

    /**
     * create a RuntimeCryptoException with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public RuntimeCryptoException(
        String  message)
    {
        super(message);
    }
}
