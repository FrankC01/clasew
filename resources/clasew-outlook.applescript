
script safe_caller
	property args : null
	property scpt_res : {}

	# Routine (debug)

	on parm_dump()
		log args
		log scpt_res
	end parm_dump

	# Routine to determine if app is running and, if not, start it


	(* Entry point for script, invoked on the 'run script' command. Expects the argList to be a list
	of records where each record is a handler descriptor
	arg_list 		- Argument list of lists, each item passed to the relative handler in handler_list
	*)

	on run (argList)
		set args to argList
		return it
	end run

  on cleanval(val)
    local oval
    set oval to null
    if val is not equal to missing value then
      set oval to val
	  end if
	  return oval
  end cleanval

	# Handler lookup and call
	# Each record in the arg list is a handler descriptor with following keys
	# handler_name refers to the handler string name used in the if/then case
	# handler_args refers to the argument data structure supported by the handler

	on call_handler()
		local a_handler, a_arglist, foo
		repeat with j from 1 to count of args
			set myblock to item j of args
			repeat with i from 1 to count of myblock
				set a_handler to handler_name of item i of myblock
				set a_arglist to handler_args of item i of myblock

				if a_handler is not null then

					if a_handler = "clasew_run" then
            set end of scpt_res to (run script a_arglist)

          else if a_handler = "clasew_get_all_identities" then
            set end of scpt_res to my clasew_get_all_identities(a_arglist)

          else if a_handler = "clasew_quit" then
            set end of scpt_res to my clasew_quit(a_arglist)

					else
						error "ERROR: Handler " & a_handler & " not found!"
					end if
				end if
			end repeat
		end repeat
	end call_handler

  on clasew_get_all_identities(arguments)
  tell application "Microsoft Outlook"
    local res7368
    set res7368 to {}
    repeat with G__7311 in contacts
      local add_list
      set add_list to {}
      set indy to {primary_department:null, first_name:null, name_suffix:null, middle_name:null, primary_company:null, primary_title:null, last_name:null, full_name:null}
      set primary_department of indy to my cleanval(department of G__7311)
      set first_name of indy to my cleanval(first name of G__7311)
      set name_suffix of indy to my cleanval(suffix of G__7311)
      set middle_name of indy to my cleanval(middle name of G__7311)
      set primary_company of indy to my cleanval(company of G__7311)
      set primary_title of indy to my cleanval(job title of G__7311)
      set last_name of indy to my cleanval(last name of G__7311)
      set full_name of indy to my cleanval(display name of G__7311)

      set G__7321 to {zip_code:null, street_name:null, city_name:null, country_name:null, state_name:null}
      set zip_code of G__7321 to my cleanval(home zip of G__7311)
      set street_name of G__7321 to my cleanval(home street address of G__7311)
      set city_name of G__7321 to my cleanval(home city of G__7311)
      set country_name of G__7321 to my cleanval(home country of G__7311)
      set state_name of G__7321 to my cleanval(home state of G__7311)

      set G__7322 to {zip_code:null, street_name:null, city_name:null, country_name:null, state_name:null}
      set zip_code of G__7322 to my cleanval(business zip of G__7311)
      set street_name of G__7322 to my cleanval(business street address of G__7311)
      set city_name of G__7322 to my cleanval(business city of G__7311)
      set country_name of G__7322 to my cleanval(business country of G__7311)
      set state_name of G__7322 to my cleanval(business state of G__7311)

      set end of add_list to G__7321
      set end of add_list to G__7322
      set indy to indy & {address_list:add_list}
      set end of res7368 to indy
    end repeat
    return res7368
  end tell
end clasew_get_all_identities

	on clasew_quit(arguments)
		try
			tell application "Microsoft Outlook" to quit
			return {clasew_quit:"success"}
		on error
			return {clasew_quit:"fail"}
		end try
	end clasew_quit

end script

(* Handler entry point for script management *)

on clasew_eval(arg_map_list)
	set res to {}

	repeat with i from 1 to count of arg_map_list
		set tmpMap to item i of arg_map_list
		set x to run script safe_caller with parameters {tmpMap}
		x's call_handler()
		set end of res to scpt_res of x
	end repeat

	return res
end clasew_eval
