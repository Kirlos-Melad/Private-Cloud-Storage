# Real-Time DB:

	"Users":
		$id:
			"Name": $name
			"Email": $email
			"About": $about
			"ProfilePicture": $url
			"Groups":
				$group_id: $name

	"Groups":
		$id:
			"Owner": $user_id
			"Name": $name
			"Password": $password
			"Description": $description

			"AllOnline": $boolean
			"OnlineCounter": $counter
			"Members":
				$user_id: 
					"Status": ["Online", "Offline"]
					"Vote"
						$user_id: $boolean

			"SharedFiles":
				$file_id:
					"Mode": ["Normal", "Striping"]
					"PreviousName": $previous_name
					"Name": $name
					"URL": $url
					"SeenBy":
						$user_id: $name
			"RecycledFiles":
				$file_id: $file_name

	"Files":
		$id:
			"Mode": ["Normal", "Striping"]
			"Group": $group_id
			"URL": $url
			$version_number:
				"Name": $name
				"Date": $date
				"StorageMetadata": $file_metadata
				"Change": ["New", "Renamed", "Modified"]


## IMPORTANT NOTE:
Files URLs MUST be concatenated with \$version_number to be added in SharedFile

SharedFiles URLs MUST be concatenated with \[\$file_id, \$file_id + ' ' + \$user_id\] depending on the MODE to be able to refernce the file to be downloaded

---

# Cloud Storage:
	Groug Files:
		$group_id/$file_id/$version_number/[$file_id, $file_id + ' ' + $user_id]
	User Files:
		$user_id/$file_name

---

# Physical Storage:
	-> $application_directory
		-> "Users"
			-> $user_id + " " + $user_name
				-> "User Files"
					-> $file_name
				-> "Groups"
					-> group_id + " " + $group_name
						-> "Normal Files"
							-> $file_name
						-> "Merged Files"
							-> $file_name
						-> "Stripped Files"
							-> $file_name

---

# Naming Convension:
	Interface Name: IFirstSecond
	Class Name: FirstSecond
	Class Java Member: mFirstSecond
	Class Android Member: _FirstSecond
	Class Functions: FirstSecond
	Local Variables: firstSecond
	XML Variables: FirstSecond
	Git Branch Name: [first-second, IssueLabel-IssueSeverityLevel-IssueNumber]
