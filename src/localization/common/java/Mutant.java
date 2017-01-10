package localization.common.java;

/**
 * This class used to communicate with mutation tools. A mutant records the
 * start line number and end line number of a source file, which will be
 * replaced by the mutation source
 * 
 * @author Jiajun
 *
 */
public class Mutant {
	/**
	 * start line number
	 */
	private int _start;
	/**
	 * end line number
	 */
	private int _end;
	/**
	 * mutated source code to replace the source code between {@code _start} and
	 * {@code _end}
	 */
	private String _source;

	/**
	 * 
	 * @param start
	 *            : start line number
	 * @param end
	 *            : end line number
	 */
	public Mutant(int start, int end) {
		_start = start;
		_end = end;
	}

	public int getStartLineNumber() {
		return _start;
	}

	public int getEndLineNumber() {
		return _end;
	}

	public void setSource(String source) {
		_source = source;
	}

	public String getReplaceSource() {
		return _source;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _end;
		result = prime * result + ((_source == null) ? 0 : _source.hashCode());
		result = prime * result + _start;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mutant other = (Mutant) obj;
		if (_end != other._end || _start != other._start)
			return false;
		if (_source == null) {
			if (other._source != null)
				return false;
		} else if (!_source.equals(other._source))
			return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "Mutant [_start=" + _start + ", _end=" + _end + ", _source=" + _source + "]";
	}
	

}
