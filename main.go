package main

import (
	"crypto/md5"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"flag"
	"fmt"
	"log"
	"os"
)

func generateSalt() ([4]byte, error) {
	salt := [4]byte{}
	_, err := rand.Read(salt[:])
	salt = [4]byte{0,0,0,0}
	return salt, err
}

func generateHashSha256(salt [4]byte, password string) ([]byte){
	temp_hash := sha256.Sum256(append(salt[:], []byte(password)...))
	return temp_hash[:]
}

func generateHashMd5(salt [4]byte, password string) ([]byte){
	temp_hash := md5.Sum(append(salt[:], []byte(password)...))
	return temp_hash[:]
}


func main() {
	// variables
	var algorithm string
	var salt = [4]byte{}
	var password string
	var hash []byte
	var help bool

	// define flags
	flag.StringVar(&algorithm,"algorithm","sha256", "Hash algorithm sha256 or md5")
	flag.StringVar(&password,"password","", "Password")
	flag.BoolVar(&help,"help",false, "This help information")

	// define default message
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "Generate RabbitMQ password hash\n")
		fmt.Fprintf(os.Stderr, "Usage of %s:\n", os.Args[0])
		flag.PrintDefaults()
	}

	// parse command line parameters
	flag.Parse()

	if help == true {
		flag.Usage()
		os.Exit(0)
	}

	if password == "" {
		os.Exit(10)
	}

	salt, err := generateSalt()
	if err != nil {
		log.Fatal(err)
	}

	switch algorithm {
	case "sha256":
		hash = generateHashSha256(salt,password)
	case "md5":
		hash = generateHashMd5(salt,password)
	default:
		flag.Usage()
		os.Exit(20)
	}

	hash = append(salt[:],[]byte(hash[:])...)
	fmt.Println(base64.StdEncoding.EncodeToString(hash[:]))
}