
script safe_caller
	property args : null
	property scpt_res : {}

	# Routine (debug)

	on parm_dump()
		log args
		log scpt_res
	end parm_dump

	# Routine to determine if app is running and, if not, start it


	(* Entry point for script, invoked on the 'run script' command.
		Expects the argList to be a list
		of scripts where each script is a handler descriptor
		arg_list - Argument list of scripts, each item is executed
	*)

	on run (argList)
		set args to argList
		return it
	end run


	# Handler lookup and call
	# Each record in the arg list is a handler descriptor with following keys
	# handler_name refers to the handler string name used in the if/then case
	# handler_args refers to the argument data structure supported by the handler

	on call_handler()
		local a_script
		repeat with j from 1 to count of args
			set a_script to item j of args
			set end of scpt_res to (run script item j of args)
		end repeat
	end call_handler

end script

(* Handler entry point for script management *)

on clasew_eval(arg_list)
	local res, my_script
	set res to {}
	repeat with i from 1 to count of arg_list
		set my_script to item i of arg_list
    try
	    set end of res to (run script my_script)
    on error errStr number errorNumber
      set end of res to errStr
    end try
	end repeat
	return res
end clasew_eval2
