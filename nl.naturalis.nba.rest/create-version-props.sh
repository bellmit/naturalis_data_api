#!/bin/bash

prop_file="${1}/version.properties"

git_tag=$(git describe --abbrev=0)
git_commit=$(git rev-parse --verify HEAD)
git_branch=$(git rev-parse --abbrev-ref HEAD)
build_date=$(date '+%Y-%m-%d %H:%M:%S')
build_number=1

echo Creating version.properties in ${1}

echo "# Generated by created-version-props.sh at ${build_date}" > "${prop_file}"
echo git.tag=${git_tag} >> "${prop_file}"
echo git.commit=${git_commit} >> "${prop_file}"
echo git.branch=${git_branch} >> "${prop_file}"
echo build.date=${build_date} >> "${prop_file}"
if [ -f .buildno ]
then
	build_number=$(($(cat .buildno)+1))
fi
echo ${build_number} > .buildno
echo build.number=${build_number} >> "${prop_file}"

 