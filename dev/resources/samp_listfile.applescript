on filename_list(relpath_from_home)
	tell application "Finder"
		set fPath to (POSIX path of (path to home folder)) & relpath_from_home as POSIX file
		set the_files to (list folder fPath without invisibles)
	end tell
	return the_files
end filename_list

