# input folder of pdfs
# pass it to the grobid training data generator module
# pass the generated un processed training data to the mapper
# dump the correct processed training data in a folder
import argparse
import os
import shutil

parser = argparse.ArgumentParser()
parser.add_argument('--gP', required=True, help='grobid directory')
parser.add_argument('--dIn', required=True, help='aplus directory')
# parser.add_argument('--gOut', required=True, help='out directory for grobid generated files')
args = parser.parse_args()

# get the input files pdfs and the xmls
for d in os.listdir(args.dIn):
    dir_path = os.path.join(args.dIn,d)
    for f in os.listdir(dir_path):
        src_path = os.path.join(dir_path, f)
        if f.endswith('.pdf'):
            target_dir = "./TrainingData/Input_pdf"
            shutil.copy(src_path,target_dir)
        elif f.endswith('.xml'):
            target_dir = "./TrainingData/Aplus_xml"
            shutil.copy(src_path,target_dir)

grobid_jar = os.path.join(args.gP, 'grobid-core/target/grobid-core-0.4.1.one-jar.jar')
grobid_home =  os.path.join(args.gP, 'grobid-home')
gOut = "./TrainingData/grobid_data"
# dIn = "/home/aman/data/training/auto/input_data_pdfs"
# dOut = "/home/aman/data/training/auto/grobid_files"
cmd1 = "java -Xmx1024m -jar {} -gH {} -dIn {} -dOut {} -exe createTrainingHeader".format(grobid_jar, grobid_home, args.dIn, gOut)
os.system(cmd1)
# run mapper
grobid_dir = gOut
out_files_dir = "./TrainingData/Corrected_data"
xml_ref_dir = "./TrainingData/Aplus_xml"
cmd2 = "python /home/aman/PycharmProjects/mapper/mapper2.py -pG {} -pO {} -pA {}".format(grobid_dir, out_files_dir, xml_ref_dir)
os.system(cmd2)
