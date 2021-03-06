script safe_caller
	property hand : null
	property args : null
	property wkbkName : null
	property wkbkDName : null
	property create_if : false
	property open_if : false
	property wkbkPath : null
	property wkbkObj : null
	property scpt_res : null
	property create_scrpt : null

	# Routine (debug)

	on parm_dump()
		log hand
		log args
		log wkbkName
		log wkbkDName
		log create_if
		log open_if
		log wkbkPath
		log scpt_res
		log name of create_scrpt
	end parm_dump

	# Routine to determine if app is running and, if not, start it

	on app_run()
		set success to false
		try
			if application "Microsoft Excel" is running then
				set success to true
			else
				tell application "Microsoft Excel"
					launch
					set success to true
				end tell
			end if
		on error
			set success to false
		end try
		return success
	end app_run

	# Routine for checking if the supplied workbook (name) is open

	on wkbk_loaded()
		set wkbk_loaded to true
		try
			tell application "Microsoft Excel"
				set wkbkObj to workbook wkbkName
				if wkbkObj = missing value then
					set wkbk_loaded to false
				else
					set wkbk_loaded to true
				end if
			end tell
		on error errMsg
			log "EXCEPTION: " & errMsg
			set wkbk_loaded to false
		end try
		return wkbk_loaded
	end wkbk_loaded

	# Routine for opening workbook of choice

	on open_wkbk()
		set wkbk_loaded to true
		set fqn to (POSIX file wkbkPath & wkbkName) as text
		try
			tell application "Microsoft Excel"
				open fqn
				set wkbkObj to workbook wkbkName
				set end of scpt_res to {open_wkbk:wkbkName & " success"}
			end tell
		on error errMsg
			set wkbk_loaded to false
			set end of scpt_res to {open_wkbk:wkbkName & " " & errMsg}
		end try
		return wkbk_loaded
	end open_wkbk

	# Routine for creating a new workbook

	on create_wkbk()
		set wkbk_loaded to true
		set fqn to (POSIX file wkbkPath & wkbkName) as text
		try
			tell application "Microsoft Excel"
				set wkbkObj to make new workbook
				tell wkbkObj
					save workbook as filename fqn overwrite yes
				end tell
				set wkbkObj to workbook wkbkName
				set end of scpt_res to {create_wkbk:wkbkName & " success"}
			end tell
		on error errMsg
			set wkbk_loaded to false
			set end of scpt_res to {create_wkbk:wkbkName & " " & errMsg}
		end try
		return wkbk_loaded
	end create_wkbk

	# Setup tables on sheet from create or add sheet

	on set_sheet_tables(t_sheet, t_list)
		if (count of t_list) > 0 then
			tell application "Microsoft Excel"
				local h_range, f_range, show_headers, t_name
				tell t_sheet
					repeat with atable in t_list
						set show_headers to false
						set f_range to range (full_range of atable)
            set t_name to table_name of atable
						if (count header_content of atable) > 0 then
							set h_range to range (header_range of atable)
							set value of h_range to header_content of atable
							set show_headers to true
						end if
						make new list object at end with properties ¬
            {name: t_name,range object:f_range, show headers:show_headers}
					end repeat
				end tell
			end tell
		end if
	end set_sheet_tables

	on create_wkbk_withparms(create_args)
		local wkbk_created, t_name, t_list
		-- set t_name to template_name of create_args
		set t_list to table_list of create_args
		set wkbk_created to true
		set fqn to (POSIX file wkbkPath & wkbkName) as text

		try
			tell application "Microsoft Excel"
				set wkbkObj to make new workbook --  with properties {document template:template t_name}

				tell wkbkObj
					set name of sheet 1 to sheet_name of create_args
					my set_sheet_tables(sheet 1, t_list)
					save workbook as filename fqn overwrite yes
				end tell
				set wkbkObj to workbook wkbkName
				-- my wkbk_loaded()
				set end of scpt_res to {create_wkbk:wkbkName & " success"}
			end tell
		on error errMsg
			set wkbk_created to false
			set end of scpt_res to {create_wkbk:wkbkName & " " & errMsg}
		end try
		return wkbk_created
	end create_wkbk_withparms

	# Indirection to create based on parameters set or not

	on create_document(myprops)

		script old_create
			return my create_wkbk()
		end script
		script new_create
			property create_parms : myprops
			return create_wkbk_withparms(create_parms)
		end script
		if (count of myprops) is 0 then
			return old_create
		else
			return new_create
		end if
	end create_document

	on path_converter(input)
		try
			if first item of input = "/" then
				input
			else
				POSIX path of (run script input)
			end if
		on error
			return "Failed"
		end try
	end path_converter


	(* Entry point for script, invoked on the 'run script' command. Expects the argList to be a list
	containing a record consisting of:
	handler_list - a list containing names of handler requests, will execute in order
	arg_list 		- Argument list of lists, each item passed to the relative handler in handler_list
	work_book 	- Name of workbook to reference or null (rare)
	create_ifm 	- Boolean to indicate that workbook should be created if not found
	open_ifm 	- Boolean to indicate that workbook should b opened if not found
	fqn_path 	- Fully qualified filename in posix format (e.g. "~\Desktop\clasew-ex4.xlsx")
	*)

	on run (argList)
		local setup
		set setup to item 1 of argList
		set scpt_res to {}
		set hand to handler_list of setup
		set args to arg_list of setup
		set wkbkName to work_book of setup
		set create_if to create_ifm of setup as boolean
		set open_if to open_ifm of setup as boolean
		set wkbkPath to my path_converter(fqn_path of setup)
		set create_scrpt to my create_document(create_parms of (setup & {create_parms:{}}))


		if wkbkPath = "Failed" then
			error "Unresolvable path statement"
		end if
		return it
	end run

	(* invoke validates environment, following directions as set for workbook and,
	if successful, runs the handler requested *)

	on invoke()
		set appres to true
		set is_valid to true
		set appres to app_run()
		if appres and (wkbkName is not null) then
			set is_valid to wkbk_loaded()
			if is_valid is false then
				if create_if and wkbkPath is not null then
					set is_valid to run create_scrpt -- create_wkbk()
				else if open_if and wkbkPath is not null then
					set is_valid to open_wkbk()
				else
					error "Unresolvable path statement"
				end if
			end if
		end if
		if is_valid then
			return call_handler()
		end if
	end invoke

	# Handler lookup and call

	on call_handler()

		repeat with i from 1 to count of hand
			set a_handler to item i of hand
			set a_arglist to item i of args
			if a_handler is not null then

				-- ***************
				-- Range functions
				-- ***************

				if a_handler = "clasew_get_range_info" then
					set end of scpt_res to clasew_get_range_info(a_arglist)

				else if a_handler = "clasew_get_range_data" then
					set end of scpt_res to clasew_get_range_data(a_arglist)

				else if a_handler = "clasew_get_range_formulas" then
					set end of scpt_res to clasew_get_range_formulas(a_arglist)

				else if a_handler = "clasew_put_range_data" then
					set end of scpt_res to clasew_put_range_data(a_arglist)

					-- ******************
					-- Worksheet functions
					-- ******************

				else if a_handler = "clasew_add_sheet" then
					set end of scpt_res to clasew_add_sheet(a_arglist)

				else if a_handler = "clasew_delete_sheet" then
					set end of scpt_res to clasew_delete_sheet(a_arglist)

					-- ******************
					-- Workbook functions
					-- ******************

				else if a_handler = "clasew_get_book_info" then
					set end of scpt_res to clasew_get_book_info(a_arglist)

				else if a_handler = "clasew_get_all_book_info" then
					set end of scpt_res to clasew_get_all_book_info(a_arglist)

				else if a_handler = "clasew_save" then
					set end of scpt_res to clasew_save(a_arglist)

				else if a_handler = "clasew_save_as" then
					set end of scpt_res to clasew_save_as(a_arglist)

				else if a_handler = "clasew_save_and_quit" then
					set end of scpt_res to clasew_save_and_quit(a_arglist)

				else if a_handler = "clasew_quit" then
					set end of scpt_res to clasew_quit(a_arglist)

				else if a_handler = "clasew_quit_no_save" then
					set end of scpt_res to clasew_quit_no_save(a_arglist)

					-- ******************
					-- Misc. functions
					-- ******************

				else if a_handler = "clasew_run" then
					set end of scpt_res to clasew_run(a_arglist)

				else
					error "ERROR: Handler " & hand & " not found!"
				end if
			end if
		end repeat
	end call_handler

	(* User arbitrary script handler *)

	on clasew_run(arguments)
		set scpt to item 1 of arguments
		set scpt_args to item 2 of arguments
		return run script scpt with parameters {me, scpt_args}
	end clasew_run

	(* clasew_get_book_info returns information for single workbook.
    if no argument, then assumes current workbook *)

	on clasew_get_book_info(arguments)
		local myBook
		if (count of arguments) is 0 then
			set myBook to wkbkObj
		else
			set myBook to item 1 of arguments
		end if
		tell application "Microsoft Excel"
			tell myBook
				local res
				set res to {book_name:name, fqn:full name, book_sheets:{}}
				repeat with sheetz in (get every sheet as list)
					set s_info to {sheet_name:name of sheetz, sheet_tables:{}}
					repeat with tablez in (get every list object of sheetz as list)
						local my_range, range_text, header_range
						set my_range to range object of tablez
						set header_range to header row of tablez
						set range_text to get address of my_range without column absolute and row absolute
						set end of sheet_tables of s_info to ¬
							{table_name:name of tablez, table_range:range_text, used_range:null, header_columns:count of rows of header_range, header_rows:0}
					end repeat
					set end of book_sheets of res to s_info
				end repeat
				return res
			end tell
		end tell
	end clasew_get_book_info

	(* clasew_get_all_book_info returns information about all the open workbooks at
	the time it is called *)

	on clasew_get_all_book_info(arguments)
		local myRes
		set myRes to {}
		tell application "Microsoft Excel"
			set thebooks to workbooks
			repeat with bookclz in thebooks
				set the end of myRes to my clasew_get_book_info({bookclz})
			end repeat
		end tell
		return myRes
	end clasew_get_all_book_info

	(* clasew_get_range_info returns information about a range in curent
  workbook specific sheet *)

	on clasew_get_range_info(arguments)
		local sheetname, myRes, my_range
		set sheetname to item 1 of arguments
		set myRes to ¬
			{range_start:0, range_end:0, first_row:0, first_col:0, count_rows:0, count_cols:0}
		tell application "Microsoft Excel"
			tell worksheet sheetname of wkbkObj
				if (item 2 of arguments) = "used" then
					set my_range to used range
				else
					set my_range to range (item 2 of arguments)
				end if
				try
					local start_row, start_col, row_count, col_count
					-- get key values
					set start_row to first row index of my_range
					set start_col to first column index of my_range
					set row_count to count of rows of my_range
					set col_count to count of columns of my_range
					-- set results record
					set first_row of myRes to start_row - 1
					set first_col of myRes to start_col - 1
					set count_rows of myRes to row_count
					set count_cols of myRes to col_count
					set range_start of myRes to ¬
						(get address of cell start_row of column start_col ¬
							without column absolute and row absolute)
					set range_end of myRes to ¬
						(get address of cell row_count of column col_count ¬
							without column absolute and row absolute)
				end try
			end tell
		end tell
		return {clasew_get_range_info:myRes}
	end clasew_get_range_info

	on clasew_get_range_data(arguments)
		local work_sheet, str_range, my_range
		set work_sheet to item 1 of arguments
		set str_range to item 2 of arguments
		tell application "Microsoft Excel"
			if str_range = "used" then
				set my_range to used range of sheet work_sheet of wkbkObj
			else
				set my_range to range str_range of sheet work_sheet of wkbkObj
			end if
			return {clasew_get_range_data:value of my_range}
		end tell
	end clasew_get_range_data

	on clasew_get_range_formulas(arguments)
		local work_sheet, str_range, my_range
		set work_sheet to item 1 of arguments
		set str_range to item 2 of arguments
		tell application "Microsoft Excel"
			if str_range = "used" then
				set my_range to used range of sheet work_sheet of wkbkObj
			else
				set my_range to range str_range of sheet work_sheet of wkbkObj
			end if
			return {clasew_get_range_formulas:formula of my_range}
		end tell
	end clasew_get_range_formulas

	on clasew_put_range_data(arguments)
		set work_sheet to item 1 of arguments
		set str_range to item 2 of arguments
		set value_list to item 3 of arguments
		tell application "Microsoft Excel"
			set myrange to range str_range of sheet work_sheet of wkbkObj
			set value of myrange to value_list
			return {clasew_put_range_data:"success"}
		end tell
	end clasew_put_range_data

	(* SHEET OPERATIONS *)

	on verify_sheet_references(arguments, map)
		local s_names, del_list, i_val
		set s_names to map's sheet_names
		set del_list to {valid_sheets:{}, oor:{}, nnf:{}}
		repeat with i from 1 to (count arguments)
			set i_val to item i of arguments
			if class of i_val is integer then
				if i_val ≤ (count of s_names) then
					set end of valid_sheets of del_list to item i_val of s_names
				else
					set end of oor of del_list to i_val
				end if
			else
				if s_names contains i_val then
					set end of valid_sheets of del_list to i_val
				else
					set end of nnf of del_list to i_val
				end if
			end if
		end repeat
		return del_list
	end verify_sheet_references

	on get_before_sheet(offset_value)
		local my_sheets
		tell application "Microsoft Excel"
			set my_sheets to wkbkObj's worksheets
			if (count of my_sheets) ≤ offset_value then
				return name of last item of my_sheets
			else
				return name of item offset_value of my_sheets
			end if
		end tell
	end get_before_sheet

	on clasew_add_sheet(arguments)
		local my_name, my_position, my_relative, made_sheet
		tell application "Microsoft Excel"
			repeat with foo in arguments
				set my_name to new_sheet of foo
				set my_position to target of foo
				set my_relative to relative_to of foo
        set t_list to table_list of foo
				if my_position is -1 then
					make new worksheet with properties {name:my_name} at before worksheet my_relative of wkbkObj
				else if my_position is 1 then
					make new worksheet with properties {name:my_name} at after worksheet my_relative of wkbkObj
				else if my_position is 0 then
					if my_relative is 0 then
						make new worksheet with properties {name:my_name} at beginning of wkbkObj
					else if my_relative is -1 then
						make new worksheet with properties {name:my_name} at end of wkbkObj
					else
						make new worksheet with properties {name:my_name} at before worksheet (my get_before_sheet(my_relative)) of wkbkObj
					end if
				end if
        tell wkbkObj
          set made_sheet to worksheet my_name
          my set_sheet_tables(made_sheet, t_list)
        end tell
			end repeat
			return {clasew_add_sheet:name of wkbkObj's worksheets}
		end tell
	end clasew_add_sheet

	on clasew_delete_sheet(arguments)
		local s_map, res
		set s_map to my clasew_get_book_info({})
		set res to my verify_sheet_references(arguments, s_map)
		tell application "Microsoft Excel"
			set display alerts to false
			repeat with del_sheet in valid_sheets of res
				delete worksheet del_sheet of my wkbkObj
			end repeat
			set display alerts to true
		end tell
		return {clasew_delete_sheet:res}
	end clasew_delete_sheet

	(* WORKBOOK AND APPLICATION OPERATIONS *)

	on clasew_save(arguments)
		try
			tell application "Microsoft Excel" to save wkbkObj
			return {clasew_save:wkbkName}
		on error
			return {clasew_save:"fail"}
		end try
	end clasew_save

	on clasew_save_as(arguments)
		set targ_file to item 1 of arguments
		set targ_path to item 2 of arguments
		set fqn to (POSIX file path_converter(targ_path) & targ_file) as text
		tell application "Microsoft Excel"
			tell wkbkObj
				save workbook as filename fqn overwrite yes
			end tell
			return {clasew_save_as:fqn}
		end tell
	end clasew_save_as

	on clasew_quit(arguments)
		try
			tell application "Microsoft Excel" to quit
			return {clasew_quit:"success"}
		on error
			return {clasew_quit:"fail"}
		end try
	end clasew_quit

	on clasew_quit_no_save(arguments)
		try
			tell application "Microsoft Excel" to quit saving no
			return {clasew_quit_no_save:"success"}
		on error
			return {clasew_quit_no_save:"failed"}
		end try
	end clasew_quit_no_save

	on clasew_save_and_quit(arguments)
		my clasew_save(arguments)
		my clasew_quit(arguments)
	end clasew_save_and_quit

end script

(* Handler entry point for script management *)

on clasew_eval(arg_map_list)
	set res to {}

	repeat with i from 1 to count of arg_map_list
		set tmpMap to item i of arg_map_list
		set x to run script safe_caller with parameters {tmpMap}
		x's invoke()
		x's parm_dump()
		set end of res to scpt_res of x
	end repeat

	return res
end clasew_eval
