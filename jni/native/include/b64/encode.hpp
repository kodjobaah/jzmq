// :mode=c++:
/*
encode.h - c++ wrapper for a base64 encoding algorithm

This is part of the libb64 project, and has been placed in the public domain.
For details, see http://sourceforge.net/projects/libb64
*/
#ifndef BASE64_ENCODE_H
#define BASE64_ENCODE_H


#define BUFFERSIZE 16777216

 namespace base64
{
	extern "C" 
	{
		#include "cencode.h"
	}

	struct encoder
	{
		base64_encodestate _state;
		int _buffersize;

		encoder(int buffersize_in = BUFFERSIZE): _buffersize(buffersize_in) {}

		int encode(char value_in)
		{
			return base64_encode_value(value_in);
		}

		int encode(const char* code_in, const int length_in, char* plaintext_out)
		{
			return base64_encode_block(code_in, length_in, plaintext_out, &_state);
		}

		int encode_end(char* plaintext_out)
		{
			return base64_encode_blockend(plaintext_out, &_state);
		}

	};

} // namespace base64

#endif // BASE64_ENCODE_H

