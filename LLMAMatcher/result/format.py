import os
import re

# read a txt file from path
# remove all lines between <think> and </think>

def read_file(path):
    with open(path, 'r', encoding='utf-8') as file:
        content = file.read()
    return content

def remove_think_tags(content):
    # Use regex to remove everything between the closest <think> and </think>
    pattern = r'<think>.*?</think>'
    cleaned_content = re.sub(pattern, '', content, flags=re.DOTALL)
    return cleaned_content

def write_file(path, content):
    with open(path, 'w', encoding='utf-8') as file:
        file.write(content)


readme_path = os.path.join(os.path.dirname(__file__), 'Anatomy/qwen3:8b/Human/llm_selected_correspondences/human_mouse_llm_selected_correspondences-0.9-formated.txt')
if os.path.exists(readme_path):
    readme_content = read_file(readme_path)
    cleaned_content = remove_think_tags(readme_content)
    write_file(readme_path, cleaned_content)