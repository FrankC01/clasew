script safe_caller_nunb
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
			if application "Numbers" is running then
				set success to true
			else
				tell application "Numbers"
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
		tell application "Numbers"
			local myapp, myinfo
			set myapp to file of every document
			repeat with ifile in myapp
				set myinfo to info for ifile
				if wkbkName is equal to name of myinfo then
					set wkbkDName to displayed name of myinfo
					set wkbkObj to document wkbkDName
					return true
				end if
			end repeat
			return false
		end tell
	end wkbk_loaded

	# Routine for opening workbook of choice

	on open_wkbk()
		local wkbk_opened, wkbk_result
		set wkbk_opened to true
		set fqn to (POSIX file wkbkPath & wkbkName) as text
		if wkbk_opened then
			try
				fqn as alias
				tell application "Numbers"
					set wkbk_result to open fqn
					if wkbk_result = missing value then
						set wkbk_opened to false
					else
						my wkbk_loaded()
						set end of scpt_res to {open_wkbk:wkbkName & " success"}
					end if
				end tell
			on error errMsg
				set wkbk_opened to false
				set end of scpt_res to {open_wkbk:errMsg}
			end try
		end if
		return wkbk_opened
	end open_wkbk

	# Routine for creating a new workbook - version 0.1.9

	on create_wkbk()
		local wkbk_created
		set wkbk_created to true
		set fqn to (POSIX file wkbkPath & wkbkName) as text
		try
			tell application "Numbers"
				set wkbkObj to make new document
				save wkbkObj in file fqn
				my wkbk_loaded()
				set end of scpt_res to {create_wkbk:wkbkName & " success"}
			end tell
		on error errMsg
			set wkbk_created to false
			set end of scpt_res to {create_wkbk:wkbkName & " " & errMsg}
		end try
		return wkbk_created
	end create_wkbk

	on set_sheet_tables(t_sheet, t_list)
		if (count of t_list) > 0 then
			tell application "Numbers"
				tell t_sheet
					delete every table
					repeat with atable in t_list
						make new table with properties ¬
							{name:table_name of atable,¬
              row count:row_count of atable, column count:column_count of atable, ¬
              header row count:header_row_count of atable, ¬
              header column count:header_column_count of atable}
            if (count header_content of atable) > 0 then
              my clasew_put_range_data({name,header_range of atable,¬
                          {header_content of atable}, table_name of atable})
            end if
					end repeat
				end tell
			end tell
		end if
	end set_sheet_tables

	on create_wkbk_withparms(create_args)
		local wkbk_created, t_name, t_list
		set t_name to template_name of create_args
		set t_list to table_list of create_args
		set wkbk_created to true
		set fqn to (POSIX file wkbkPath & wkbkName) as text
		try
			tell application "Numbers"
				set wkbkObj to make new document with properties {document template:template t_name}
				tell wkbkObj
					set name of sheet 1 to sheet_name of create_args
					my set_sheet_tables(sheet 1, t_list)
				end tell
				save wkbkObj in file fqn
				my wkbk_loaded()
				set end of scpt_res to {create_wkbk:wkbkName & " success"}
			end tell
		on error errMsg
			set wkbk_created to false
			set end of scpt_res to {create_wkbk:wkbkName & " " & errMsg}
		end try
		return wkbk_created
	end create_wkbk_withparms

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
		set hand to handler_list of setup
		set args to arg_list of setup
		set wkbkName to work_book of setup
		set create_if to create_ifm of setup as boolean
		set open_if to open_ifm of setup as boolean
		set wkbkPath to my path_converter(fqn_path of setup)
		set create_scrpt to my create_document(create_parms of (setup & {create_parms:null}))

		set scpt_res to {}
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
			else

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
		local f_info, s_info, res
		if (count of arguments) is 0 then
			set myBook to wkbkObj
		else
			set myBook to item 1 of arguments
		end if
		tell application "Numbers"
			set f_info to (info for (get file of myBook))
			set res to {book_name:name of myBook, fqn:displayed name of f_info, book_sheets:{}}
			repeat with sheetz in (get every sheet of myBook)
				set s_info to {sheet_name:(get name of sheetz), sheet_tables:{}}
				repeat with tablez in (get every table of sheetz)
					set end of sheet_tables of s_info to ¬
						{table_name:name of tablez, table_range:name of cell range of tablez,¬
            used_range:null, header_columns:header column count of tablez, ¬
            header_rows:header row count of tablez}
				end repeat
				set end of book_sheets of res to s_info
			end repeat
			return {get_book_info:res}
		end tell
	end clasew_get_book_info

	(* clasew_get_all_book_info returns information about all the open workbooks at
	the time it is called *)

	on clasew_get_all_book_info(arguments)
		set myRes to {}
		tell application "Numbers"
			set thebooks to documents
			repeat with bookclz in thebooks
				set the end of myRes to my clasew_get_book_info({bookclz})
			end repeat
		end tell
		return myRes
	end clasew_get_all_book_info

	(* clasew_get_range_info returns information about a range in curent workbook specific sheet *)

	on valid_range(arguments)
		local sheet_name, my_range, range_str, sheetz, tablez
		set sheet_name to item 1 of arguments
		set range_str to item 2 of arguments
		try
			tell application "Numbers"
				set sheetz to sheet sheet_name of wkbkObj
				if (count of arguments) < 3 then
					set tablez to first table of sheetz
				else
					set tablez to table (item 3 of arguments) of sheetz
				end if
				tell tablez
					set my_range to range range_str -- figure out 'used'
				end tell
			end tell
			return tablez
		on error errMsg
      set end of scpt_res to name of errMsg
			return null
		end try
	end valid_range

	on get_range_values(tablez, str_range, val_else_form)
		local my_range, r_list, varg, x_list
		set r_list to {}
		tell application "Numbers"
			if tablez is not equal to null then
				tell tablez
          local i_list, i_row, i_val
					set my_range to range str_range

          -- Get the whole range as a list of lists

					if val_else_form then
            set x_list to value of cells of every row of my_range
					else
            set x_list to formula of cells of every row of my_range
					end if

          -- For each row of data
					repeat with r_r from 1 to (count of x_list)
            set i_row to item r_r of x_list
	  				set i_list to {}

            -- For each value in row
            -- Check for missing value and replace with empty string
            repeat with cv in i_row
              set i_val to cv as text
              if i_val = "missing value" then
                set end of i_list to ""
              else
                set end of i_list to i_val
              end if
            end repeat
						set end of r_list to i_list
					end repeat
					return r_list
				end tell
			else
				return str_range & " is outside range of table"
			end if
		end tell
	end get_range_values

	on clasew_get_range_info(arguments)
		local sheetname, myRes, my_range, range_str, sheetz, tablez, varg
		set sheetname to item 1 of arguments
		set range_str to item 2 of arguments
		set varg to {sheetname, str_range}
		if (count of arguments) > 2 then set end of varg to item 3 of arguments
		if my valid_range(varg) then
			set myRes to ¬
				{range_start:0, range_end:0, first_row:0, first_col:0, count_rows:0, count_cols:0}
			tell application "Numbers"
				set sheetz to sheet sheetname of wkbkObj
				if (count of arguments) < 3 then
					set tablez to first table of sheetz
				else
					set tablez to table (item 3 of arguments) of sheetz
				end if

				tell tablez
					set my_range to range range_str -- figure out 'used'
					set first_row of myRes to ((address of row 1 of my_range) - 1)
					set first_col of myRes to ((address of column 1 of my_range) - 1)
					set count_rows of myRes to count of rows of my_range
					set count_cols of myRes to count of columns of my_range
					set range_start of myRes to (get name of column 1 of my_range) & (get name of row 1 of my_range)
					set range_end of myRes to (get name of last column of my_range) & (get name of last row of my_range)
				end tell
			end tell
		else
			set myRes to str_range & " outside range of table "
		end if
		return {clasew_get_range_info:myRes}
	end clasew_get_range_info


	on clasew_get_range_data(arguments)
		local sheetname, str_range, varg
		set sheetname to item 1 of arguments
		set str_range to item 2 of arguments
		set varg to {sheetname, str_range}
		if (count of arguments) > 2 then set end of varg to item 3 of arguments
		tell application "Numbers"
			return {clasew_get_range_data:my get_range_values(my valid_range(varg), str_range, true)}
		end tell
	end clasew_get_range_data

	on clasew_get_range_formulas(arguments)
		local sheetname, str_range, varg
		set sheetname to item 1 of arguments
		set str_range to item 2 of arguments
		set varg to {sheetname, str_range}
		if (count of arguments) > 2 then set end of varg to item 3 of arguments
		tell application "Numbers"
			return {clasew_get_range_formulas:my get_range_values(my valid_range(varg), str_range, false)}
		end tell
	end clasew_get_range_formulas

	on clasew_put_range_data(arguments)
		local sheetname, str_range, value_list, sheetz, tablez, my_range, sheetz, tablez, varg
		set sheetname to item 1 of arguments
		set str_range to item 2 of arguments
		set value_list to item 3 of arguments
		set varg to {sheetname, str_range}
		if (count of arguments) > 3 then set end of varg to item 4 of arguments
		set tablez to my valid_range(varg)
		if tablez is not null then
			tell application "Numbers"
				set sheetz to sheet sheetname of wkbkObj
				tell tablez
					local s_row
					set my_range to range str_range
					set s_row to ((address of row 1 of my_range) - 1)
					repeat with i_row from 1 to (count of value_list)
						local i_val
						set i_val to item i_row of value_list
						repeat with i_col from 1 to (count of i_val)
							set r_row to row i_row of my_range
							set value of cell (s_row + i_row) of column i_col of my_range to item i_col in i_val
						end repeat
					end repeat
					-- set value of my_range to (first item of value_list)
					return {clasew_put_range_data:"success"}
				end tell
			end tell
		else
			return {clasew_put_range_data:str_range & " outside range of table "}
		end if
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
		tell application "Numbers"
			set my_sheets to wkbkObj's sheets
			if (count of my_sheets) ≤ offset_value or offset_value < 1 then
				return name of last item of my_sheets
			else
				return name of item offset_value of my_sheets
			end if
		end tell
	end get_before_sheet

	on new_first_sheet(before_sheet_name, my_props)
		tell application "Numbers"
			activate
			local current_first, tempSheetCopyName, current_copy, new_sheet
			tell wkbkObj
				set current_first to sheet before_sheet_name
				set active sheet to current_first
				set tempSheetCopyName to "Copy Of " & before_sheet_name

				-- Duplicate the current first sheet
				tell application "System Events"
					-- deselect all
					keystroke "a" using {command down, shift down}
					delay 0.1
					-- select all containers (tables, text items, etc.)
					keystroke "a" using {command down}
					delay 0.1
					-- copy the containers
					keystroke "c" using {command down}
					delay 0.1
				end tell

				set current_copy to make new sheet with properties {name:tempSheetCopyName}
				set active sheet to current_copy

				tell current_copy
					delete every table
					delay 0.1
					tell application "System Events"
						keystroke "v" using {command down}
					end tell
					delay 0.1
				end tell

				-- Insert the new sheet after the current first
				set active sheet to current_first
				set new_sheet to make new sheet with properties my_props

				set active sheet to new_sheet

				-- Delete the original
				delete current_first

				-- Rename the copy back to the original name
				set name of current_copy to before_sheet_name

				return new_sheet
			end tell
		end tell
	end new_first_sheet

	on clasew_add_sheet(arguments)
		local my_name, my_position, my_relative, made_sheet, t_list
		tell application "Numbers"
			repeat with foo in arguments
				set my_name to new_sheet of foo
				set my_position to target of foo
				set my_relative to relative_to of foo
				set t_list to table_list of foo
				tell wkbkObj
					if my_position is -1 then -- Get location of named sheet as active then create the sheet before it
						my new_first_sheet(my_relative, {name:my_name})
						set made_sheet to sheet my_name
						-- move new_sheet to ???
					else if my_position is 1 then -- Get location of named sheet as active then create the sheet after it
						set active sheet to sheet my_relative
						set made_sheet to make new sheet with properties {name:my_name}
					else if my_position is 0 then
						if my_relative is 0 then -- Make new sheet the first one
							my new_first_sheet(get name of sheet 1, {name:my_name})
							set made_sheet to sheet 1
						else if my_relative is -1 then -- Make the new sheet the last one
							set active sheet to sheet (count of sheets)
							set made_sheet to make new sheet with properties {name:my_name}
						else
							set active sheet to sheet (my get_before_sheet(my_relative - 1))
							set made_sheet to make new sheet with properties {name:my_name}
						end if
					end if
					my set_sheet_tables(made_sheet, t_list)
				end tell
			end repeat
			return {clasewl_add_sheet:name of wkbkObj's sheets}
		end tell
	end clasew_add_sheet

	on clasew_delete_sheet(arguments)
		local s_map, res
		set s_map to my clasew_get_book_info({})
		set res to my verify_sheet_references(arguments, s_map)
		tell application "Numbers"
			tell wkbkObj
				repeat with del_sheet in valid_sheets of res
					delete sheet del_sheet
				end repeat
			end tell
		end tell
		return {deleted_sheets:res}
	end clasew_delete_sheet

	(* WORKBOOK AND APPLICATION OPERATIONS *)

	on clasew_save(arguments)
		try
			tell application "Numbers" to save wkbkObj
			return {clasew_save:wkbkName}
		on error
			return {clasew_save:"fail"}
		end try
	end clasew_save

	on clasew_save_as(arguments)
		set targ_file to item 1 of arguments
		set targ_path to item 2 of arguments
		set fqn to (POSIX file path_converter(targ_path) & targ_file) as text
		tell application "Numbers"
			save wkbkObj in file fqn
		end tell
	end clasew_save_as

	on clasew_quit(arguments)
		try
			tell application "Numbers" to quit
			return {clasew_quit:"success"}
		on error
			return {clasew_quit:"fail"}
		end try
	end clasew_quit

	on clasew_quit_no_save(arguments)
		try
			tell application "Numbers" to quit saving no
			return {clasew_quit_no_save:"success"}
		on error
			return {clasew_quit_no_save:"failed"}
		end try
	end clasew_quit_no_save

	on clasew_save_and_quit(arguments)
		clasew_save(arguments)
		clasew_quit(arguments)
	end clasew_save_and_quit

end script

(* Handler entry point for script management *)

on clasew_eval(arg_map_list)
	set res to {}

	repeat with i from 1 to count of arg_map_list
		set tmpMap to item i of arg_map_list
		set x to run script safe_caller_nunb with parameters {tmpMap}
		x's invoke()
		set end of res to scpt_res of x
	end repeat

	return res
end clasew_eval
